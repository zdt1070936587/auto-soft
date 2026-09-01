package com.autosoft.agent.studio;

import com.autosoft.agent.config.StudioUploadProperties;
import com.autosoft.agent.entity.AiAttachmentDO;
import com.autosoft.agent.mapper.AiAttachmentMapper;
import com.autosoft.agent.vo.AiAttachmentVO;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 工作室附件：上传、校验、入模内容组装。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class StudioAttachmentService {

    public static final int MAX_PER_SEND = 5;
    public static final int MAX_TEXT_BYTES = 2 * 1024 * 1024;
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    public static final int TEXT_INJECT_LIMIT = 12000;

    private static final Set<String> TEXT_EXT = Set.of("json", "txt", "md", "csv");
    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private final AiAttachmentMapper attachmentMapper;
    private final Path uploadRoot;

    public StudioAttachmentService(AiAttachmentMapper attachmentMapper, StudioUploadProperties properties) {
        this.attachmentMapper = attachmentMapper;
        this.uploadRoot = Path.of(properties.getDir()).toAbsolutePath().normalize();
    }

    public AiAttachmentVO upload(Long sessionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        String kind = resolveKind(ext);
        long size = file.getSize();
        if ("text".equals(kind) && size > MAX_TEXT_BYTES) {
            throw new BizException(ResultCode.BAD_REQUEST, "文本附件不能超过 2MB");
        }
        if ("image".equals(kind) && size > MAX_IMAGE_BYTES) {
            throw new BizException(ResultCode.BAD_REQUEST, "图片附件不能超过 5MB");
        }
        try {
            Files.createDirectories(uploadRoot);
            String stored = sessionId + "/" + UUID.randomUUID() + "." + ext;
            Path target = uploadRoot.resolve(stored).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BizException(ResultCode.BAD_REQUEST, "非法路径");
            }
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            AiAttachmentDO row = new AiAttachmentDO();
            row.setSessionId(sessionId);
            row.setFileName(original);
            row.setContentType(file.getContentType() == null ? guessContentType(ext) : file.getContentType());
            row.setSizeBytes(size);
            row.setKind(kind);
            row.setStoragePath(stored);
            attachmentMapper.insert(row);
            return toVo(row);
        } catch (IOException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "附件保存失败");
        }
    }

    public List<AiAttachmentDO> requireForSend(Long sessionId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        if (attachmentIds.size() > MAX_PER_SEND) {
            throw new BizException(ResultCode.BAD_REQUEST, "单次最多 5 个附件");
        }
        List<AiAttachmentDO> rows = attachmentMapper.selectList(new LambdaQueryWrapper<AiAttachmentDO>()
                .eq(AiAttachmentDO::getSessionId, sessionId)
                .in(AiAttachmentDO::getId, attachmentIds));
        if (rows.size() != attachmentIds.size()) {
            throw new BizException(ResultCode.BAD_REQUEST, "附件不存在或不属于当前会话");
        }
        return rows;
    }

    public void linkToMessage(Long sessionId, Long messageId, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        attachmentMapper.update(null, new LambdaUpdateWrapper<AiAttachmentDO>()
                .eq(AiAttachmentDO::getSessionId, sessionId)
                .in(AiAttachmentDO::getId, attachmentIds)
                .set(AiAttachmentDO::getMessageId, messageId));
    }

    public List<AiAttachmentDO> listByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return attachmentMapper.selectList(new LambdaQueryWrapper<AiAttachmentDO>()
                .in(AiAttachmentDO::getMessageId, messageIds));
    }

    public String buildPersistContent(String userText, List<AiAttachmentDO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return userText;
        }
        StringBuilder sb = new StringBuilder(userText);
        for (AiAttachmentDO attachment : attachments) {
            sb.append("\n\n[附件: ").append(attachment.getFileName()).append(']');
            if ("text".equals(attachment.getKind())) {
                sb.append('\n').append(readTextSnippet(attachment));
            }
        }
        return sb.toString();
    }

    public Map<String, Object> buildUserMessage(String userText, List<AiAttachmentDO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Map.of("role", "user", "content", userText);
        }
        List<Object> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder(userText);
        for (AiAttachmentDO attachment : attachments) {
            if ("text".equals(attachment.getKind())) {
                text.append("\n\n[附件: ").append(attachment.getFileName()).append("]\n")
                        .append(readTextSnippet(attachment));
            }
        }
        parts.add(Map.of("type", "text", "text", text.toString()));
        for (AiAttachmentDO attachment : attachments) {
            if ("image".equals(attachment.getKind())) {
                parts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", toDataUrl(attachment))));
            }
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", "user");
        item.put("content", parts);
        return item;
    }

    public Map<String, Object> buildUserMessageFromStored(String content, List<AiAttachmentDO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Map.of("role", "user", "content", content == null ? "" : content);
        }
        boolean hasImage = attachments.stream().anyMatch(a -> "image".equals(a.getKind()));
        if (!hasImage) {
            return Map.of("role", "user", "content", content == null ? "" : content);
        }
        List<Object> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", content == null ? "" : content));
        for (AiAttachmentDO attachment : attachments) {
            if ("image".equals(attachment.getKind())) {
                try {
                    parts.add(Map.of(
                            "type", "image_url",
                            "image_url", Map.of("url", toDataUrl(attachment))));
                } catch (Exception ex) {
                    parts.set(0, Map.of("type", "text", "text",
                            content + "\n[图片附件: " + attachment.getFileName() + "，当前模型不支持图片解析]"));
                }
            }
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("role", "user");
        item.put("content", parts);
        return item;
    }

    public List<AiAttachmentVO> toVoList(List<AiAttachmentDO> rows) {
        return rows.stream().map(this::toVo).collect(Collectors.toList());
    }

    private AiAttachmentVO toVo(AiAttachmentDO source) {
        AiAttachmentVO vo = new AiAttachmentVO();
        vo.setId(source.getId());
        vo.setFileName(source.getFileName());
        vo.setContentType(source.getContentType());
        vo.setSizeBytes(source.getSizeBytes());
        vo.setKind(source.getKind());
        return vo;
    }

    private String readTextSnippet(AiAttachmentDO attachment) {
        try {
            String raw = Files.readString(resolve(attachment));
            if (raw.length() > TEXT_INJECT_LIMIT) {
                return raw.substring(0, TEXT_INJECT_LIMIT) + "\n...(truncated)";
            }
            return raw;
        } catch (IOException ex) {
            return "(无法读取附件内容)";
        }
    }

    private String toDataUrl(AiAttachmentDO attachment) {
        try {
            byte[] bytes = Files.readAllBytes(resolve(attachment));
            String mime = attachment.getContentType() == null ? "image/png" : attachment.getContentType();
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "读取图片失败");
        }
    }

    private Path resolve(AiAttachmentDO attachment) {
        Path path = uploadRoot.resolve(attachment.getStoragePath()).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法附件路径");
        }
        return path;
    }

    private static String extension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的文件类型");
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private static String resolveKind(String ext) {
        if (TEXT_EXT.contains(ext)) {
            return "text";
        }
        if (IMAGE_EXT.contains(ext)) {
            return "image";
        }
        throw new BizException(ResultCode.BAD_REQUEST, "仅支持 json/txt/md/csv 文本与 png/jpg/jpeg/webp/gif 图片");
    }

    private static String guessContentType(String ext) {
        return switch (ext) {
            case "json" -> "application/json";
            case "md" -> "text/markdown";
            case "csv" -> "text/csv";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "text/plain";
        };
    }

    public void purgeSession(Long sessionId) {
        List<AiAttachmentDO> rows = attachmentMapper.selectList(new LambdaQueryWrapper<AiAttachmentDO>()
                .eq(AiAttachmentDO::getSessionId, sessionId));
        for (AiAttachmentDO row : rows) {
            if (row.getStoragePath() == null || row.getStoragePath().isBlank()) {
                continue;
            }
            try {
                Files.deleteIfExists(resolve(row));
            } catch (Exception ignored) {
                // 磁盘文件缺失不影响删会话
            }
        }
        attachmentMapper.delete(new LambdaQueryWrapper<AiAttachmentDO>()
                .eq(AiAttachmentDO::getSessionId, sessionId));
    }
}
