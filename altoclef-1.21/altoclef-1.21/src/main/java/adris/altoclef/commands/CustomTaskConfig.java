package adris.altoclef.commands;

class CustomTaskConfig {
    public String prefix = "custom2";
    public CustomTaskEntry[] customTasks = new CustomTaskEntry[0];

    static class CustomTaskEntry {
        public String name;
        public String description;
        public CustomSubTaskEntry[] tasks;

        static class CustomSubTaskEntry {
            public String command;
            public String[][] parameters;
        }
    }
}
