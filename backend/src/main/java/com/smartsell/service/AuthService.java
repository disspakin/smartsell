    package com.smartsell.service;

    import com.smartsell.dto.AuthResponse;
    import com.smartsell.dto.LoginRequest;
    import com.smartsell.dto.RegisterRequest;
    import com.smartsell.dto.ResetPasswordRequest;
    import com.smartsell.entity.AppUser;
    import com.smartsell.exception.EmailAlreadyUsedException;
    import com.smartsell.exception.InvalidCredentialsException;
    import com.smartsell.exception.InvalidInviteCodeException;
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

        /**
         * รหัสเชิญที่ต้องแนบมาตอนสมัคร — อ่านจาก app.auth.invite-code
         * (หรือ environment variable APP_AUTH_INVITE_CODE) ห้าม hardcode ไว้ในโค้ด
         * ถ้าไม่ได้ตั้งค่า จะปฏิเสธทุกคำขอ (fail closed) แทนที่จะเปิดให้สมัครได้ทุกคน
         */
        private final String inviteCode;

        public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           AuthThrottleService throttleService,
                           @Value("${app.auth.invite-code:}") String inviteCode) {
            this.appUserRepository = appUserRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtUtil = jwtUtil;
            this.throttleService = throttleService;
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

            try {
                appUserRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                // สองคำขอที่ใช้อีเมลเดียวกันเข้ามาพร้อมกัน — findByEmail ข้างบนผ่านทั้งคู่
                // แต่ unique constraint ที่คอลัมน์ email จะสกัดตัวที่สองไว้ตรงนี้
                throw new EmailAlreadyUsedException("อีเมลนี้ถูกใช้สมัครแล้ว");
            }

            throttleService.recordRegistration(clientIp);
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
            return toAuthResponse(user);
        }

        /**
         * ตั้งรหัสผ่านใหม่โดยยืนยันด้วย invite code (ไม่มีระบบส่งอีเมล)
         *
         * ตรวจ invite code ก่อนแตะฐานข้อมูล invite code ผิดจึงได้ 403 เสมอ
         * ไม่ว่าอีเมลนั้นจะมีบัญชีหรือไม่ ส่วนเมื่อ code ถูก ก็ไม่บอกว่าเจอบัญชีหรือเปล่า
         * ผู้เรียกจึงใช้ endpoint นี้ไล่เดาว่าอีเมลไหนมีบัญชีอยู่ไม่ได้
         *
         * hash รหัสใหม่ก่อนค้นหาบัญชีเสมอ ให้ทั้งสองกรณีเสียเวลา BCrypt เท่ากัน
         * ไม่งั้นจับเวลาตอบกลับก็แยกออกได้ว่าอีเมลไหนมีจริง
         */
        @Transactional
        public void resetPassword(ResetPasswordRequest request, String clientIp) {
            String email = normalizeEmail(request.getEmail());

            throttleService.ensurePasswordResetAllowed(email, clientIp);
            throttleService.recordPasswordResetAttempt(email, clientIp);

            if (!inviteCodeMatches(request.getInviteCode())) {
                throw new InvalidInviteCodeException("invite code ไม่ถูกต้อง");
            }

            String newHash = passwordEncoder.encode(request.getNewPassword());
            appUserRepository.findByEmail(email).ifPresent(user -> {
                user.setPasswordHash(newHash);
                appUserRepository.save(user);
            });
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
