package br.dev.bielsolosos.biscraper.domain.notification.event;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {

    private User recipient;
    private NotificationTemplate contentTemplate;
    private Map<String, Object> items;
    private boolean transactional;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GenericNotificationTemplate implements NotificationTemplate {
        private NotificationChannel[] channels;
        private String message;
        private String subject;

        @Override
        public NotificationChannel[] getChannels() {
            if (this.channels == null || ArrayUtils.isEmpty(this.channels)) {
                return new NotificationChannel[]{NotificationChannel.DISCORD};
            }
            return this.channels;
        }

        @Override
        public String getMessageTemplate() {
            if (StringUtils.isBlank(this.message)) {
                return "";
            }
            return this.message;
        }

        @Override
        public String getSubject() {
            return this.subject != null ? this.subject : "BI Scraper - Notificação";
        }
    }

    public boolean hasTemplate() {
        return this.contentTemplate != null
                && this.contentTemplate.getChannels() != null
                && this.contentTemplate.getChannels().length > 0;
    }
}
