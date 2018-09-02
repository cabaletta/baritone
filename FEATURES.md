# Pathing features
- Long distance pathing and splicing. Baritone calculates paths in segments

# Goals
The pathing goal can be set to any of these options 
- GoalBlock - one specific block that the player should stand inside at foot level
- GoalXZ - an X and a Z coordinate, used for long distance pathing
- GoalYLevel - a Y coordinate
- GoalTwoBlocks - a block position that the player should stand in, either at foot or eye level
- GoalGetToBlock - a block position that the player should stand adjacent to, below, or on top of
- GoalNear - a block position that the player should get within a certain radius of, used for following entities

And finally GoalComposite. GoalComposite is a list of other goals, any one of which satisfies the goal. For example, `mine diamond_ore` creates a GoalComposite of GoalTwoBlock s for every diamond ore location it knows of.


