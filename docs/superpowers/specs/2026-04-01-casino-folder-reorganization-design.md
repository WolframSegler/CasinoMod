# Casino Folder Reorganization Design

## Summary

Reorganize game-specific files from `src/data/scripts/casino/` root into separate subfolders for each game type, matching the existing `poker5/` pattern.

## Target Structure

```
src/data/scripts/casino/
├── arena/
│   ├── ArenaDialogDelegate.java
│   ├── ArenaPanelUI.java
│   └── SpiralAbyssArena.java
├── blackjack/
│   ├── BlackjackDialogDelegate.java
│   ├── BlackjackGame.java
│   └── BlackjackPanelUI.java
├── cards/          (preserve existing)
├── gacha/
│   ├── CasinoGachaManager.java
│   ├── GachaAnimation.java
│   └── GachaAnimationDialogDelegate.java
├── interaction/    (preserve existing)
├── poker2/
│   ├── PokerDialogDelegate.java
│   ├── PokerGame.java
│   ├── PokerOpponentAI.java
│   ├── PokerPanelUI.java
├── poker5/         (preserve existing)
├── shared/         (preserve existing)
├── CasinoConfig.java      (stay in root)
├── CasinoDebtScript.java  (stay in root)
├── CasinoVIPManager.java  (stay in root)
├── Strings.java           (stay in root)
```

## Import Changes

### interaction/ Handlers

| File | Changes |
|------|---------|
| ArenaHandler.java | `data.scripts.casino.Arena*` → `data.scripts.casino.arena.*`, `SpiralAbyssArena` → `arena.SpiralAbyssArena` |
| BlackjackHandler.java | `data.scripts.casino.Blackjack*` → `data.scripts.casino.blackjack.*` |
| GachaHandler.java | `data.scripts.casino.Gacha*` → `data.scripts.casino.gacha.*`, `CasinoGachaManager` → `gacha.CasinoGachaManager` |
| PokerHandler.java | `data.scripts.casino.Poker*` → `data.scripts.casino.poker2.*` |

### Game Panel/UI Files

| File | Changes |
|------|---------|
| ArenaDialogDelegate.java | package → `data.scripts.casino.arena`, import `ArenaPanelUI` from same package |
| ArenaPanelUI.java | package → `data.scripts.casino.arena`, import `SpiralAbyssArena` from same package |
| BlackjackDialogDelegate.java | package → `data.scripts.casino.blackjack`, import `BlackjackPanelUI` from same package |
| BlackjackPanelUI.java | package → `data.scripts.casino.blackjack`, import `BlackjackGame` from same package |
| GachaAnimationDialogDelegate.java | package → `data.scripts.casino.gacha`, import `GachaAnimation` from same package |
| PokerDialogDelegate.java | package → `data.scripts.casino.poker2`, import `PokerPanelUI` from same package |
| PokerPanelUI.java | package → `data.scripts.casino.poker2`, import `PokerGame` from same package |

### Cross-Game Dependencies

- ArenaHandler imports `CasinoGachaManager` → update to `data.scripts.casino.gacha.CasinoGachaManager`
- ArenaPanelUI imports `SpiralAbyssArena.SpiralGladiator` → update to `data.scripts.casino.arena.SpiralAbyssArena.SpiralGladiator`

## Implementation Steps

1. Create folders: `arena/`, `blackjack/`, `gacha/`, `poker2/`
2. Move files to their respective folders
3. Update package declarations in moved files
4. Update imports in all files referencing moved classes
5. Verify no compilation errors

## Files to Keep in Root

- `CasinoConfig.java` - Global configuration, used across all games
- `CasinoDebtScript.java` - Debt system, cross-game functionality
- `CasinoVIPManager.java` - VIP system, cross-game functionality
- `Strings.java` - Localization utility, used everywhere