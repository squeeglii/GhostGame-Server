package net.cg360.spookums.server.auth.record;

import net.cg360.spookums.server.util.clean.Check;

public class SecureIdentity {

    private final String accountID;
    private final String passwordHash;
    private final String passwordSalt;
    private final long accountCreationTime;

    public SecureIdentity(String accountID, String passwordHash, String passwordSalt, long accountCreationTime) {
        Check.nullParam(accountID, "accountID");
        Check.nullParam(passwordHash, "passwordHash");
        Check.nullParam(passwordSalt, "passwordSalt");
        Check.nullParam(accountCreationTime, "accountCreationTime");

        this.accountID = accountID;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.accountCreationTime = accountCreationTime;
    }
}
