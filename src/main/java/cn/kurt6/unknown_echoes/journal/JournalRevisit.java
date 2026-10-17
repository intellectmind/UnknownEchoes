package cn.kurt6.unknown_echoes.journal;

import cn.kurt6.unknown_echoes.UnknownEchoes;
import cn.kurt6.unknown_echoes.ability.EchoAbilityType;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public enum JournalRevisit {
    TIDE_RUNES(EchoAbilityType.TIDE_ECHO, "sunken_temple", "tide_runes"),
    HIDDEN_ROOM(EchoAbilityType.TRUE_SIGHT_ECHO, "mirror_temple", "hidden_room"),
    OBSERVATORY(EchoAbilityType.WIND_ECHO, "sky_observatory", "observatory"),
    RECORD_ROOM(EchoAbilityType.TIDE_ECHO, "submerged_record_room", "record_room");

    private final EchoAbilityType ability;
    private final String structureId;
    private final String hintKey;

    JournalRevisit(EchoAbilityType ability, String structure, String hint) {
        this.ability = ability;
        this.structureId = UnknownEchoes.id(structure).toString();
        this.hintKey = "journal.unknown_echoes.revisit." + hint;
    }

    public String hintKey() {
        return hintKey;
    }

    public boolean available(Set<String> structures, Predicate<EchoAbilityType> hasAbility) {
        return structures.contains(structureId) && hasAbility.test(ability);
    }

    public ExplorationClue visitedSite(List<ExplorationClue> clues, ExplorationClue selected,
                                       String dimension, BlockPos origin) {
        List<ExplorationClue> visited = clues.stream()
                .filter(clue -> clue.visited() && clue.structureId().equals(structureId)).toList();
        for (ExplorationClue clue : visited) {
            if (clue.sameSite(selected)) return clue;
        }
        return JournalNavigation.knownSite(visited, structureId, dimension, origin);
    }
}
