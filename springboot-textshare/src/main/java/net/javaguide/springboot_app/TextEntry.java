package net.javaguide.springboot_app;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TextEntry {
    @Id
    private String id;
    private String content;

    public TextEntry() {
        // no-args constructor required by JPA
    }

    public TextEntry(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
