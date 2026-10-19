package com.fenglin.springboottest.service;

import com.fenglin.springboottest.exception.BusinessException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.UUID;

/**
 * 图形验证码：生成 4 位「字母 + 数字」干扰图（PNG），
 * 以 captchaId 为 key 在内存中保存正确文本，5 分钟过期，校验为一次性。
 */
@Service
public class CaptchaService {

    // 去除易混字符：0/O/1/I/L
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LEN = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final long TTL_MS = 5 * 60 * 1000L;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public CaptchaService() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "captcha-cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每分钟清理一次过期验证码，避免内存泄漏
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /** 生成一张新验证码，返回 captchaId 与 base64 图片（data:image/png;base64,...） */
    public Captcha generate() {
        String code = randomCode();
        BufferedImage img = draw(code);
        String b64 = encode(img);
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Entry(code, System.currentTimeMillis() + TTL_MS));
        return new Captcha(id, "data:image/png;base64," + b64);
    }

    /** 校验图形验证码（一次性：无论成败都作废，防止爆破） */
    public void validate(String captchaId, String input) {
        if (captchaId == null || input == null || input.trim().isEmpty()) {
            throw new BusinessException("请输入图形验证码");
        }
        Entry e = store.remove(captchaId);
        if (e == null) {
            throw new BusinessException("验证码已失效，请点击图片刷新");
        }
        if (System.currentTimeMillis() > e.expireAt) {
            throw new BusinessException("验证码已过期，请点击图片刷新");
        }
        if (!e.code.equalsIgnoreCase(input.trim())) {
            throw new BusinessException("图形验证码不正确");
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(en -> en.getValue().expireAt < now);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(LEN);
        for (int i = 0; i < LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage draw(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 背景
        g.setColor(new Color(244, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                       random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        // 字符：随机颜色 + 轻微旋转
        Font base = new Font("Arial", Font.BOLD, 30);
        for (int i = 0; i < LEN; i++) {
            g.setFont(base);
            g.setColor(new Color(20 + random.nextInt(120), 20 + random.nextInt(120), 110 + random.nextInt(120)));
            int x = 14 + i * 26;
            int y = 33 + random.nextInt(6) - 3;
            double theta = (random.nextDouble() - 0.5) * 0.5;
            g.rotate(theta, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-theta, x, y);
        }
        g.dispose();
        return img;
    }

    private String encode(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /** 返回给前端的验证码视图对象 */
    public record Captcha(String captchaId, String image) {}

    private static class Entry {
        final String code;
        final long expireAt;
        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
