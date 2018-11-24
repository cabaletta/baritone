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

import cabaletta.comms.downward.MessageComputationRequest;
import cabaletta.comms.upward.MessageComputationResponse;
import cabaletta.comms.upward.MessageStatus;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ComputationRequestManager {
    INSTANCE;
    public static final int MAX_SIMUL_COMPUTATIONS_PER_HOST = 2;

    private Map<Long, ComputationRequest> inProgress = new HashMap<>();
    private Map<Bot, Map<String, ComputationRequest>> byGoal = new HashMap<>();
    public Function<Bot, String> botToHostMapping = Bot::toString; // TODO map bots to hosts by connection maybe?


    public ComputationRequest getByGoal(SingularTaskLeaf task, String goal) {
        Map<String, ComputationRequest> thisBot = byGoal.computeIfAbsent(task.bot, x -> new HashMap<>());
        ComputationRequest req = thisBot.computeIfAbsent(goal, g -> new ComputationRequest(task.bot, goal));
        req.addParentTask(task);
        return req;
    }

    public void dispatchAll() {
        Map<String, List<ComputationRequest>> highestPriorityByHost = byGoal.values().stream().map(Map::values).flatMap(Collection::stream).filter(c -> c.getStatus() == ComputationRequest.Status.QUEUED).collect(Collectors.groupingBy(req -> botToHostMapping.apply(req.bot), Collectors.collectingAndThen(Collectors.toList(), l -> l.stream().sorted(Comparator.comparingDouble(ComputationRequest::priority).reversed()).collect(Collectors.toList()))));
        inProgress.values().stream().collect(Collectors.groupingBy(req -> botToHostMapping.apply(req.bot), Collectors.collectingAndThen(Collectors.counting(), count -> MAX_SIMUL_COMPUTATIONS_PER_HOST - count))).entrySet().stream().flatMap(entry -> highestPriorityByHost.get(entry.getKey()).subList(0, entry.getValue().intValue()).stream()).forEach(this::dispatch);
    }

    private void dispatch(ComputationRequest req) {
        if (inProgress(req)) {
            throw new IllegalStateException();
        }
        long key = new SecureRandom().nextLong();
        inProgress.put(key, req);
        MessageStatus status = req.bot.getMostRecentUpdate();
        req.bot.send(new MessageComputationRequest(key, status.pathStartX, status.pathStartY, status.pathStartZ, req.goal));
    }

    public void onRecv(Bot receivedFrom, MessageComputationResponse mcrn) {
        ComputationRequest req = inProgress.remove(mcrn.computationID);
        if (req == null) {
            throw new IllegalStateException("Received completed computation that we didn't ask for");
        }
        if (!Objects.equals(req.bot, receivedFrom)) {
            throw new IllegalStateException("Received completed computation from the wrong connection?????");
        }
        req.receivedResponse(mcrn);
    }

    public boolean inProgress(ComputationRequest req) {
        return inProgress.values().contains(req);
    }
}
