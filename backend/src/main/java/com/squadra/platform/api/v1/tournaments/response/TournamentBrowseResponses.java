package com.squadra.platform.api.v1.tournaments.response;

import java.util.List;

public final class TournamentBrowseResponses {
    private TournamentBrowseResponses() { }

    public record TournamentBrowse(TournamentResponses.TournamentDetails tournament,
            List<TournamentCategoryResponses.CategoryDetails> categories) { }
}
