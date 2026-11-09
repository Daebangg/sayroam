package com.daebangg.sayroam.global.mail;

public record RenderedEmail(String subject, String plainText, String html) {
    @Override
    public String toString() {
        return "RenderedEmail[content=<redacted>]";
    }
}
