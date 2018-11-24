/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior;

import baritone.Baritone;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.process.IBaritoneProcess;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.Helper;
import baritone.utils.pathing.SegmentedCalculator;
import cabaletta.comms.BufferedConnection;
import cabaletta.comms.IConnection;
import cabaletta.comms.IMessageListener;
import cabaletta.comms.downward.MessageChat;
import cabaletta.comms.downward.MessageComputationRequest;
import cabaletta.comms.iMessage;
import cabaletta.comms.upward.MessageComputationResponse;
import cabaletta.comms.upward.MessageStatus;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ControllerBehavior extends Behavior implements IMessageListener {

    public ControllerBehavior(Baritone baritone) {
        super(baritone);
    }

    private BufferedConnection conn;

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        trySend(buildStatus());
        readAndHandle();
    }

    public MessageStatus buildStatus() {
        // TODO report inventory and echest contents
        // TODO figure out who should remember echest contents when it isn't open, baritone or tenor?
        BlockPos pathStart = baritone.getPathingBehavior().pathStart();
        return new MessageStatus(
                ctx.player().posX,
                ctx.player().posY,
                ctx.player().posZ,
                ctx.player().rotationYaw,
                ctx.player().rotationPitch,
                ctx.player().onGround,
                ctx.player().getHealth(),
                ctx.player().getFoodStats().getSaturationLevel(),
                ctx.player().getFoodStats().getFoodLevel(),
                pathStart.getX(),
                pathStart.getY(),
                pathStart.getZ(),
                baritone.getPathingBehavior().getCurrent() != null,
                baritone.getPathingBehavior().getNext() != null,
                baritone.getPathingBehavior().getInProgress().isPresent(),
                baritone.getPathingBehavior().ticksRemainingInSegment().orElse(0D),
                baritone.getPathingBehavior().calcFailedLastTick(),
                baritone.getPathingBehavior().isSafeToCancel(),
                baritone.getPathingBehavior().getGoal() + "",
                baritone.getPathingControlManager().mostRecentInControl().map(IBaritoneProcess::displayName).orElse("")
        );
    }

    private void readAndHandle() {
        if (conn == null) {
            return;
        }
        try {
            List<iMessage> msgs = conn.receiveMessagesNonBlocking();
            msgs.forEach(msg -> msg.handle(this));
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public boolean trySend(iMessage msg) {
        if (conn == null) {
            return false;
        }
        try {
            conn.sendMessage(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public void connectTo(IConnection conn) {
        disconnect();
        this.conn = BufferedConnection.makeBuffered(conn);
    }

    public void disconnect() {
        if (conn != null) {
            conn.close();
        }
        conn = null;
    }

    @Override
    public void handle(MessageChat msg) { // big brain
        ChatEvent event = new ChatEvent(ctx.player(), msg.msg);
        baritone.getGameEventHandler().onSendChatMessage(event);
    }

    @Override
    public void handle(MessageComputationRequest msg) {
        BetterBlockPos start = new BetterBlockPos(msg.startX, msg.startY, msg.startZ);
        // TODO this may require scanning the world for blocks of a certain type, idk how to manage that
        Goal goal = new GoalYLevel(Integer.parseInt(msg.goal)); // im already winston
        SegmentedCalculator.calculateSegmentsThreaded(start, goal, new CalculationContext(baritone), path -> {
            if (!Objects.equals(path.getGoal(), goal)) {
                throw new IllegalStateException(); // sanity check
            }
            try {
                BetterBlockPos dest = path.getDest();
                conn.sendMessage(new MessageComputationResponse(msg.computationID, path.length(), path.totalTicks(), path.getGoal().isInGoal(dest), dest.x, dest.y, dest.z));
            } catch (IOException e) {
                // nothing we can do about this, we just completed a computation but our tenor connection was closed in the meantime
                // just discard the path we made for them =((
                e.printStackTrace(); // and complain =)
            }
        }, () -> {
            try {
                conn.sendMessage(new MessageComputationResponse(msg.computationID, 0, 0, false, 0, 0, 0));
            } catch (IOException e) {
                // same deal
                e.printStackTrace();
            }
        });
    }

    @Override
    public void unhandled(iMessage msg) {
        Helper.HELPER.logDebug("Unhandled message received by ControllerBehavior " + msg);
    }
}
