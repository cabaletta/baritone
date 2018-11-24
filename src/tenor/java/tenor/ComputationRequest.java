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

package tenor;

import cabaletta.comms.upward.MessageComputationResponse;

import java.util.HashSet;
import java.util.Set;

public class ComputationRequest {
    public final Bot bot;
    public final String goal;
    private final Set<SingularTaskLeaf> parents; // TODO quantized UGH
    private MessageComputationResponse resp;


    ComputationRequest(Bot bot, String goal) {
        this.bot = bot;
        this.goal = goal;
        this.parents = new HashSet<>();
        this.resp = null;
    }

    public MessageComputationResponse getResp() {
        return resp;
    }

    public void receivedResponse(MessageComputationResponse mcrn) {
        resp = mcrn;
        // TODO notify parent tasks?
    }

    public Status getStatus() {
        // :woke
        // make this a huge nested ternary when
        if (resp != null) {
            if (resp.pathLength == 0) {
                return Status.RECV_FAILURE;
            } else {
                return Status.RECV_SUCCESS;
            }
        } else {
            if (ComputationRequestManager.INSTANCE.inProgress(this)) {
                return Status.IN_PROGRESS;
            } else {
                return Status.QUEUED;
            }
        }
    }

    public void addParentTask(SingularTaskLeaf parent) {
        parents.add(parent);
    }

    // hmmmmm this has parent tasks and has a cost and a priority.... god forbid should i make this a SingularTaskLeaf itself?!?!??!?
    public Double cost() {
        switch (getStatus()) {
            case RECV_FAILURE:
                return 1000000000D;
            case RECV_SUCCESS:
                return resp.pathCost;
            case IN_PROGRESS:
            case QUEUED:
                return null;
        }
    }

    public double priority() {
        return parents.stream().mapToDouble(SingularTaskLeaf::priority).sum();
    }

    public enum Status {
        QUEUED,
        IN_PROGRESS,
        RECV_SUCCESS,
        RECV_FAILURE,
    }
}
