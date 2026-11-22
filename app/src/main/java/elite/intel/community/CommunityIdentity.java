package elite.intel.community;

import elite.intel.community.FirebaseClient.CallFailed;
import elite.intel.community.FirebaseClient.Session;
import elite.intel.db.managers.CommunityManager;

import java.io.IOException;
import java.time.Instant;

/**
 * This install's anonymous Firebase account: a uid that stays the same for as long as the install keeps its
 * refresh token, and an ID token renewed from it as needed.
 * <p>
 * The account names nobody. It exists so the security rules can let an install write its own two documents and
 * nothing else.
 */
final class CommunityIdentity {

    /**
     * What the token service answers for a refresh token it no longer accepts (the account was deleted, or the
     * token belongs to another project).
     */
    private static final int TOKEN_REFUSED = 400;

    private final FirebaseClient firebase;
    private final CommunityManager store;
    private Session session;

    CommunityIdentity(FirebaseClient firebase, CommunityManager store) {
        this.firebase = firebase;
        this.store = store;
    }

    /**
     * A session good for at least another minute: the one in hand, a refreshed one, or a new account when there is
     * no usable refresh token.
     */
    Session session(Instant now) throws IOException, InterruptedException {
        if (session != null && session.isFresh(now)) return session;
        session = renewOrSignUp();
        store.saveIdentity(session.uid(), session.refreshToken());
        return session;
    }

    private Session renewOrSignUp() throws IOException, InterruptedException {
        String refreshToken = store.refreshToken();
        if (refreshToken == null) return firebase.signUp();
        try {
            return firebase.refresh(refreshToken);
        } catch (CallFailed e) {
            if (e.status != TOKEN_REFUSED) throw e;
            return firebase.signUp();
        }
    }
}
