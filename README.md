This fork of baritone is aimed mainly for mapart.
Download at: https://github.com/CornholioNeedsRolio/baritone/releases/tag/v0.0.1

If there are any issues that bother the original creator with this fork, I will remove it without hesitation

Changes this fork brings:

New settings:

	-prioritizeBottomBlocks: it should in theory prioritize the bottom blocks
	-costPerLevel: this is in direct connection with the prioritizeBottomBlocks, this is highly experimental, but it increses the cost per each level, shouldn't go high, go max around 6-8
	
	-buildInLayersSideways: if buildinlayers is true, this will build in layers on the x,z axis, if it's false, it will build on the y axis like it was before
	-buildInLayersSidewaysXAxis: it's in direct connection with the buildInLayersSideways setting, if this is false it will build on z axis, otherwise on the x axis
	
	-dontPlaceBlocksThatAreNotFull: this option should make baritone not place incomplete blocks(carpets, cobwebs, slabs, etc), to avoid a pesky bug(where it gets stuck in an infinite loop of placing and breaking), place them yourself, I had a fix to avoid it getting in infinite loops but it was just disgusting, I have to familiarize even more with bartione's source code to fix that, until then again place them yourself