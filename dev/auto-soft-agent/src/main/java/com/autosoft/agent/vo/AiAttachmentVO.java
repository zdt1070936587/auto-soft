package com.autosoft.agent.vo;

/**
 * AiAttachment视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class AiAttachmentVO {

    private Long id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String kind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
