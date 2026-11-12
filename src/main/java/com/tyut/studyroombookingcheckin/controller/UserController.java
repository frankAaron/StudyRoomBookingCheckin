package com.tyut.studyroombookingcheckin.controller;

import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.Result;
import com.tyut.studyroombookingcheckin.dto.PasswordUpdateDTO;
import com.tyut.studyroombookingcheckin.dto.ProfileUpdateDTO;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.security.SecurityContextHolder;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    /** 头像访问前缀，与 WebMvcConfig 的资源映射保持一致 */
    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    private final UserMapper userMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        User user = userMapper.selectById(SecurityContextHolder.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(toProfile(user));
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody ProfileUpdateDTO dto) {
        Long userId = SecurityContextHolder.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String nickname = dto.getNickname() == null ? "" : dto.getNickname().trim();
        if (nickname.isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        if (nickname.length() > 20) {
            throw new BusinessException("昵称长度不能超过 20 个字符");
        }
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }

        User update = new User();
        update.setId(userId);
        update.setNickname(nickname);
        update.setEmail(email);
        update.setPhone(phone);
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
        return Result.success(toProfile(userMapper.selectById(userId)));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordUpdateDTO dto) {
        if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
            throw new BusinessException("请输入原密码");
        }
        String newPassword = dto.getNewPassword() == null ? "" : dto.getNewPassword();
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException("新密码长度需为 6-32 位");
        }
        User user = userMapper.selectById(SecurityContextHolder.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (BCrypt.checkpw(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(BCrypt.hashpw(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
        return Result.<Void>success(null);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("图片大小不能超过 2MB");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext)) {
            throw new BusinessException("仅支持 JPG / PNG 格式的图片");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持 JPG / PNG 格式的图片");
        }

        Long userId = SecurityContextHolder.getUserId();
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("avatars");
        String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + ext;
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("头像保存失败，请稍后重试");
        }

        User old = userMapper.selectById(userId);
        String url = AVATAR_URL_PREFIX + fileName;
        User update = new User();
        update.setId(userId);
        update.setAvatar(url);
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
        deleteOldAvatar(old == null ? null : old.getAvatar(), target);
        return Result.success(url);
    }

    private Map<String, Object> toProfile(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("studentNo", user.getStudentNo());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("totalStudyMinutes", user.getTotalStudyMinutes());
        data.put("maxStreakDays", user.getMaxStreakDays());
        data.put("createTime", user.getCreateTime());
        return data;
    }

    private String resolveExtension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteOldAvatar(String oldUrl, Path current) {
        if (oldUrl == null || !oldUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }
        String oldName = oldUrl.substring(AVATAR_URL_PREFIX.length());
        if (oldName.isEmpty() || oldName.contains("/") || oldName.contains("..")) {
            return;
        }
        try {
            Path oldFile = current.getParent().resolve(oldName);
            if (!oldFile.equals(current)) {
                Files.deleteIfExists(oldFile);
            }
        } catch (IOException ignored) {
            // 旧文件删除失败不影响本次上传结果
        }
    }
}
