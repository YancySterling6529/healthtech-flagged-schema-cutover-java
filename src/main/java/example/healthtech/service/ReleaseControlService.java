package example.healthtech.service;

import example.healthtech.infra.InfraiClient;
import example.healthtech.infra.JsonBody;

public final class ReleaseControlService {
    private final InfraiClient infrai;
    private final String flagKey;
    private boolean migrationPathEnabled;

    public ReleaseControlService(InfraiClient infrai, String flagKey) {
        this.infrai = infrai;
        this.flagKey = flagKey;
        this.migrationPathEnabled = false;
    }

    public void enableMigrationPath() throws Exception {
        infrai.flags.toggle(flagKey, JsonBody.flagToggleBody(true, flagKey));
        infrai.flags.rollout(flagKey, JsonBody.flagRolloutBody(flagKey));
        migrationPathEnabled = true;
    }

    public boolean isMigrationPathEnabled() {
        return migrationPathEnabled;
    }
}
