package net.saturn.murderMystery.events;

public enum EventType {
    BLACKOUT("Blackout", "§8§lThe lights go out..."),
    FLICKER("Flicker", "§7§lThe lights begin to flicker!"),
    FOG("Fog", "§b§lA thick fog rolls in..."),
    ALARM("Alarm", "§e§lAn alarm has been triggered!"),
    SWAP("Teleport Swap", "§d§lThe world shifts around you!"),
    BODY_FOUND("Body Found", "§c§lA body has been discovered!");

    private final String name;
    private final String announcement;

    EventType(String name, String announcement) {
        this.name = name;
        this.announcement = announcement;
    }

    public String getName() { return name; }
    public String getAnnouncement() { return announcement; }
}