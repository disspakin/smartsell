    package com.smartsell.service;

    import com.smartsell.dto.AuthResponse;
    import com.smartsell.dto.LoginRequest;
    import com.smartsell.dto.RegisterRequest;
    import com.smartsell.dto.ResetPasswordRequest;
    import com.smartsell.entity.AppUser;
    import com.smartsell.entity.OtpPurpose;
    import com.smartsell.exception.EmailAlreadyUsedException;
    import com.smartsell.exception.EmailNotVerifiedException;
    import com.smartsell.exception.InvalidCredentialsException;
    import com.smartsell.exception.InvalidInviteCodeException;
    import com.smartsell.exception.InvalidOtpException;
    import com.smartsell.repository.AppUserRepository;
    import com.smartsell.security.AuthThrottleService;
    import com.smartsell.security.JwtUtil;
    import com.smartsell.security.Roles;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.dao.DataIntegrityViolationException;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.nio.charset.StandardCharsets;
    import java.security.MessageDigest;
    import java.util.Locale;

    /**
     * ตรรกะการสมัคร/เข้าสู่ระบบทั้งหมดอยู่ที่นี่ controller ทำหน้าที่แค่รับ-ส่ง HTTP
     */
    @Service
    public class AuthService {

        private final AppUserRepository appUserRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthThrottleService throttleService;
        private final EmailOtpService otpService;

        /**
         * รหัสเชิญที่ต้องแนบมาตอนสมัคร — อ่านจาก app.auth.invite-code
         * (หรือ environment variable APP_AUTH_INVITE_CODE) ห้าม hardcode ไว้ในโค้ด
         * ถ้าไม่ได้ตั้งค่า จะปฏิเสธทุกคำขอ (fail closed) แทนที่จะเปิดให้สมัครได้ทุกคน
         */
        private final String inviteCode;

        public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           AuthThrottleService throttleService, EmailOtpService otpService,
                           @Value("${app.auth.invite-code:}") String inviteCode) {
            this.appUserRepository = appUserRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtUtil = jwtUtil;
            this.throttleService = throttleService;
            this.otpService = otpService;
            this.inviteCode = inviteCode == null ? "" : inviteCode.trim();
        }

        /**
         * สมัครบัญชีพนักงาน/เจ้าของร้านใหม่
         *
         * ไม่ออก token ให้โดยตั้งใจ — ผู้สมัครต้องไปล็อกอินเองอีกครั้ง
         * นอกจากตรงกับ flow ที่ต้องการแล้ว ยังแปลว่า endpoint ที่เปิดสาธารณะตัวนี้
         * ไม่เคยสร้าง credential ที่ใช้งานได้ออกมาเลย ลดผลกระทบถ้ามีคนยิงมันรัวๆ
         *
         * role ถูก hardcode เป็น STORE_MANAGER ที่นี่ ไม่ได้รับมาจาก request
         * ผู้เรียก API จึงยกระดับสิทธิ์ตัวเองเกินกว่านี้ไม่ได้
         *
         * ตรวจ invite code ก่อนเช็คอีเมลซ้ำ คนที่ไม่มี code จึงใช้คำตอบ 409
         * ไล่เดาว่าอีเมลไหนมีบัญชีอยู่แล้วไม่ได้
         *
         * บัญชีใหม่ยังล็อกอินไม่ได้จนกว่าจะกรอก OTP ที่ส่งไปทางอีเมลถูก (ดู EmailOtpService)
         */
        @Transactional
        public AppUser register(RegisterRequest request, String clientIp) {
            throttleService.ensureRegisterAllowed(clientIp);

            if (!inviteCodeMatches(request.getInviteCode())) {
                throttleService.recordRegisterFailure(clientIp);
                throw new InvalidInviteCodeException("invite code ไม่ถูกต้อง");
            }

            String email = normalizeEmail(request.getEmail());

            if (appUserRepository.findByEmail(email).isPresent()) {
                throw new EmailAlreadyUsedException("อีเมลนี้ถูกใช้สมัครแล้ว");
            }

            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setDisplayName(request.getDisplayName().trim());
            user.setRole(Roles.STORE_MANAGER);
            user.setEmailVerified(false);

            try {
                appUserRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                // สองคำขอที่ใช้อีเมลเดียวกันเข้ามาพร้อมกัน — findByEmail ข้างบนผ่านทั้งคู่
                // แต่ unique constraint ที่คอลัมน์ email จะสกัดตัวที่สองไว้ตรงนี้
                throw new EmailAlreadyUsedException("อีเมลนี้ถูกใช้สมัครแล้ว");
            }

            throttleService.recordRegistration(clientIp);
            otpService.issueFor(user, OtpPurpose.VERIFY_EMAIL);
            return user;
        }

        @Transactional(readOnly = true)
        public AuthResponse login(LoginRequest request, String clientIp) {
            String email = normalizeEmail(request.getEmail());

            // ตรวจก่อนแตะฐานข้อมูล คำขอที่ถูกล็อกอยู่จึงไม่กิน query และไม่กิน BCrypt
            // ซึ่งจงใจให้ช้า การปล่อยให้ยิงรัวๆ จะกลายเป็นช่องทำให้เซิร์ฟเวอร์ล่มได้
            throttleService.ensureLoginAllowed(email, clientIp);

            AppUser user = appUserRepository.findByEmail(email).orElse(null);
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throttleService.recordLoginFailure(email, clientIp);
                throw new InvalidCredentialsException("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
            }

            throttleService.clearLoginFailures(email, clientIp);

            // เช็คหลังตรวจรหัสผ่านแล้วเท่านั้น คนที่ไม่รู้รหัสจึงใช้คำตอบนี้ไล่ดูว่าบัญชีไหนยังไม่ยืนยันไม่ได้
            if (!user.isEmailVerified()) {
                throw new EmailNotVerifiedException("กรุณายืนยันอีเมลด้วยรหัส OTP ก่อนเข้าสู่ระบบ");
            }
            return toAuthResponse(user);
        }

        /**
         * ขั้นที่ 1 ของการตั้งรหัสผ่านใหม่ — ส่ง OTP ไปที่อีเมลของเจ้าของบัญชี
         *
         * เดิมยืนยันด้วย invite code ซึ่งพนักงานทุกคนรู้ร่วมกัน ใครก็ตั้งรหัสใหม่ให้บัญชีคนอื่นได้
         * ตอนนี้ต้องเปิดกล่องจดหมายของบัญชีนั้นได้จริงเท่านั้น
         *
         * นับโควตาทุกคำขอก่อนรู้ว่ามีบัญชีหรือไม่ และไม่บอกว่าเจอบัญชีหรือเปล่า
         * ผู้เรียกจึงใช้ endpoint นี้ไล่เดาว่าอีเมลไหนมีบัญชีอยู่ไม่ได้
         */
        @Transactional
        public void requestPasswordReset(String rawEmail, String clientIp) {
            String email = normalizeEmail(rawEmail);

            throttleService.ensurePasswordResetAllowed(email, clientIp);
            throttleService.recordPasswordResetAttempt(email, clientIp);

            otpService.requestPasswordReset(email);
        }

        /**
         * ขั้นที่ 2 — ตรวจ OTP แล้วตั้งรหัสผ่านใหม่
         *
         * noRollbackFor ต้องมีที่นี่ด้วย ไม่ใช่แค่ที่ EmailOtpService เพราะ exception วิ่งผ่าน
         * transaction ชั้นนี้ขาออก ถ้าไม่ใส่ การนับครั้งที่กรอกผิดจะถูก rollback ทิ้งและเดาได้ไม่จำกัด
         *
         * กรอก OTP ที่ส่งไปทางอีเมลถูก แปลว่าเป็นเจ้าของกล่องจดหมายนั้นจริง
         * บัญชีที่ยังไม่ยืนยันอีเมลจึงถือว่ายืนยันแล้วไปด้วย ไม่ต้องกรอก OTP อีกรอบ
         */
        @Transactional(noRollbackFor = InvalidOtpException.class)
        public void resetPassword(ResetPasswordRequest request) {
            AppUser user = otpService.consumePasswordReset(request.getEmail(), request.getCode());
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setEmailVerified(true);
            appUserRepository.save(user);
        }

        /**
         * ดึงผู้ใช้จากอีเมลที่ JwtAuthFilter ใส่ไว้ใน SecurityContext
         * อ่านจาก DB ทุกครั้งแทนที่จะเชื่อ claim ใน token เพราะบัญชีอาจถูกลบหรือเปลี่ยน role หลังออก token ไปแล้ว
         */
        @Transactional(readOnly = true)
        public AppUser getUserByEmail(String email) {
            return appUserRepository.findByEmail(normalizeEmail(email))
                    .orElseThrow(() -> new InvalidCredentialsException("ไม่พบบัญชีผู้ใช้นี้"));
        }

        private AuthResponse toAuthResponse(AppUser user) {
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getDisplayName());
            return new AuthResponse(token, user.getEmail(), user.getDisplayName(), user.getRole());
        }

        /**
         * เทียบแบบใช้เวลาคงที่ (MessageDigest.isEqual) ไม่ใช่ String.equals
         * ที่หยุดทันทีเมื่อเจอตัวอักษรแรกที่ต่าง ซึ่งเปิดช่องให้จับเวลาเดา code ทีละตัว
         */
        private boolean inviteCodeMatches(String candidate) {
            if (inviteCode.isEmpty() || candidate == null) {
                return false;
            }
            return MessageDigest.isEqual(
                    inviteCode.getBytes(StandardCharsets.UTF_8),
                    candidate.trim().getBytes(StandardCharsets.UTF_8));
        }

        /** เก็บอีเมลเป็นตัวพิมพ์เล็กเสมอ เพื่อให้ Admin@utcc.ac.th กับ admin@utcc.ac.th เป็นบัญชีเดียวกัน */
        private String normalizeEmail(String email) {
            return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        }
    }
