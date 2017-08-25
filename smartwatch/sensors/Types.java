package reuiot2015.smartwatch.sensors;

/** Some types to allow type variability in sensors. */
public enum Types {
    String, Integer, Long, Float, Double, Character, Boolean;


    public String asString(Object o) {
        if (o == null) return "null";
        switch (this) {
            case String:
                return (String) o;
            case Integer:
                return ((Integer) o).toString();
            case Long:
                return ((Long) o).toString();
            case Float:
                return ((Float) o).toString();
            case Double:
                return ((Double) o).toString();
            case Character:
                return ((Character) o).toString();
            case Boolean:
                return ((Boolean) o).toString();
        }
        return "null";
    }
}
