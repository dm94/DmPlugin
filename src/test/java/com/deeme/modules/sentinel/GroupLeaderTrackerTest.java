package com.deeme.modules.sentinel;

import com.deeme.modules.genericgate.TravelMap;

/**
 * Simple standalone test (no test framework required) that validates basic GroupLeaderTracker
 * behavior using in-memory fakes. This is intended to show the expected interactions and
 * serves as a lightweight verification until full integration tests are available.
 */
public class GroupLeaderTrackerTest {
    public static void main(String[] args) {
        TravelMap travelMap = new TravelMap();
        travelMap.active = true;
        travelMap.followLeader = true;

        // fakes
        FakeGroupService groupService = new FakeGroupService();
        groupService.leaderMapId = 10;
        groupService.groupHealthPercent = 0.8;

        FakeTravelController travelController = new FakeTravelController();
        travelController.currentMapId = 8;

        GroupLeaderTracker tracker = new GroupLeaderTracker(travelMap, groupService, travelController, 0.5);
        tracker.tick();

        if (!travelController.traveledTo.contains(10)) throw new RuntimeException("Expected travel to leader map");

        // test health reaction: no leader map -> should press heal when health below threshold
        travelController.traveledTo.clear();
        groupService.leaderMapId = null;
        groupService.groupHealthPercent = 0.3;
        tracker.tick();
        if (!travelController.healPressed) throw new RuntimeException("Expected heal pressed");

        System.out.println("GroupLeaderTrackerTest: All tests passed");
    }

    static class FakeGroupService implements GroupService {
        Integer leaderMapId;
        double groupHealthPercent;
        @Override
        public Integer getLeaderMapId() { return leaderMapId; }
        @Override
        public double getGroupHealthPercent() { return groupHealthPercent; }
    }

    static class FakeTravelController implements TravelController {
        int currentMapId;
        java.util.List<Integer> traveledTo = new java.util.ArrayList<>();
        boolean healPressed = false;
        @Override
        public int getCurrentMapId() { return currentMapId; }
        @Override
        public void travelToMap(int mapId) { traveledTo.add(mapId); currentMapId = mapId; }
        @Override
        public void pressHeal() { healPressed = true; }
        @Override
        public void useAbility(String abilityId) { /* no-op */ }
    }
}
