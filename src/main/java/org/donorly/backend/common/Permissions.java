package org.donorly.backend.common;

/**
 * Permission codes used with {@code @PreAuthorize("hasAuthority(...)")}.
 * Mirrors the design document's Appendix permission codes.
 */
public final class Permissions {

    private Permissions() {
    }

    public static final String PLATFORM_ORGANIZATIONS_MANAGE = "platform.organizations.manage";
    public static final String ORG_SETTINGS_MANAGE = "org.settings.manage";
    public static final String USERS_MANAGE = "users.manage";

    public static final String DONORS_READ = "donors.read";
    public static final String DONORS_READ_ALL = "donors.read.all";
    public static final String DONORS_WRITE = "donors.write";
    public static final String DONORS_ASSIGN = "donors.assign";
    public static final String DONORS_DELETE = "donors.delete";
    public static final String DONORS_EXPORT = "donors.export";

    public static final String CAMPAIGNS_READ = "campaigns.read";
    public static final String CAMPAIGNS_MANAGE = "campaigns.manage";

    public static final String PLEDGES_READ = "pledges.read";
    public static final String PLEDGES_WRITE = "pledges.write";

    public static final String FOLLOWUPS_READ = "followups.read";
    public static final String FOLLOWUPS_WRITE = "followups.write";

    public static final String REPORTS_VIEW = "reports.view";

    public static final String PAYMENTS_MANAGE = "payments.manage";
    public static final String RECEIPTS_ISSUE = "receipts.issue";

    public static final String EVENTS_READ = "events.read";
    public static final String EVENTS_MANAGE = "events.manage";
    public static final String EVENTS_CHECKIN = "events.checkin";
    public static final String VOLUNTEERS_READ = "volunteers.read";
    public static final String VOLUNTEERS_WRITE = "volunteers.write";
    public static final String VOLUNTEERS_MANAGE = "volunteers.manage";

    public static final String TEAM_INVITE = "team.invite";   // restricted invite (ambassador-only)

    public static final String TOWNHALLS_READ = "townhalls.read";
    public static final String TOWNHALLS_MANAGE = "townhalls.manage";

    public static final String COMMUNICATIONS_READ = "communications.read";
    public static final String COMMUNICATIONS_MANAGE = "communications.manage";
    public static final String COMMUNICATIONS_SEND = "communications.send";
    public static final String AI_USE = "ai.use";
    public static final String AI_ADMIN = "ai.admin";
}
