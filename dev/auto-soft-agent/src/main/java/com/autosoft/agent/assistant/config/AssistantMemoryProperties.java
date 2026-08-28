package com.autosoft.agent.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autosoft.assistant.memory")
public class AssistantMemoryProperties {

    private boolean enabled = true;
    private String embeddingModel = "text-embedding-3-small";
    private int embeddingDimensions = 1536;
    private int injectMaxChars = 4500;
    private int episodeImportanceDefault = 5;
    private int recallTopK = 3;
    private Consolidation consolidation = new Consolidation();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(int embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public int getInjectMaxChars() {
        return injectMaxChars;
    }

    public void setInjectMaxChars(int injectMaxChars) {
        this.injectMaxChars = injectMaxChars;
    }

    public int getEpisodeImportanceDefault() {
        return episodeImportanceDefault;
    }

    public void setEpisodeImportanceDefault(int episodeImportanceDefault) {
        this.episodeImportanceDefault = episodeImportanceDefault;
    }

    public int getRecallTopK() {
        return recallTopK;
    }

    public void setRecallTopK(int recallTopK) {
        this.recallTopK = recallTopK;
    }

    public Consolidation getConsolidation() {
        return consolidation;
    }

    public void setConsolidation(Consolidation consolidation) {
        this.consolidation = consolidation;
    }

    public static class Consolidation {
        private boolean enabled = true;
        private String cron = "0 0 2 * * ?";
        private int decayFullDays = 7;
        private int decaySummaryDays = 90;
        private int operClusterPaddingMinutes = 30;
        private int batchSize = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getDecayFullDays() {
            return decayFullDays;
        }

        public void setDecayFullDays(int decayFullDays) {
            this.decayFullDays = decayFullDays;
        }

        public int getDecaySummaryDays() {
            return decaySummaryDays;
        }

        public void setDecaySummaryDays(int decaySummaryDays) {
            this.decaySummaryDays = decaySummaryDays;
        }

        public int getOperClusterPaddingMinutes() {
            return operClusterPaddingMinutes;
        }

        public void setOperClusterPaddingMinutes(int operClusterPaddingMinutes) {
            this.operClusterPaddingMinutes = operClusterPaddingMinutes;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
