package cz.humblej.squares.ui;

import cz.humblej.identity.client.AuthenticationException;
import cz.humblej.squares.auth.LeaderboardEntry;
import cz.humblej.squares.auth.LeaderboardPage;
import cz.humblej.squares.auth.OnlineAccountService;

interface LeaderboardClient {
    boolean hasSession();

    LeaderboardPage getPage(int limit, String cursor) throws AuthenticationException;

    LeaderboardEntry getMyEntry() throws AuthenticationException;

    final class Online implements LeaderboardClient {
        private final OnlineAccountService service;

        Online(OnlineAccountService service) {
            this.service = service;
        }

        @Override
        public boolean hasSession() {
            return service.hasSession();
        }

        @Override
        public LeaderboardPage getPage(int limit, String cursor)
                throws AuthenticationException {
            return service.getCasualLeaderboard(limit, cursor);
        }

        @Override
        public LeaderboardEntry getMyEntry() throws AuthenticationException {
            return service.getMyCasualLeaderboardEntry();
        }
    }
}
