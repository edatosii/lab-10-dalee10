package co.umariana.modelo;

public class Persona {

    public static final int MAX_NOMBRE = 100;

    private final String posicion;
    private final String nombre;
    private final int nacimiento;
    private final Integer fallecimiento;

    public Persona(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        this.posicion = posicion;
        this.nombre = nombre;
        this.nacimiento = nacimiento;
        this.fallecimiento = fallecimiento;
    }

    public void validar() {
        // 1. La posición debe ser válida (Ahnentafel o d'Aboville)
        if (!Posicion.esValida(posicion)) {
            throw new IllegalArgumentException("La posición no es válida: " + posicion);
        }
        
        // 2. El nombre no puede ser nulo ni estar en blanco, ni exceder MAX_NOMBRE
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni estar en blanco.");
        }
        if (nombre.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede superar los " + MAX_NOMBRE + " caracteres.");
        }
        
        // 3. El nombre no puede contener ';' ni saltos de línea
        if (nombre.contains(";") || nombre.contains("\n") || nombre.contains("\r")) {
            throw new IllegalArgumentException("El nombre no puede contener punto y coma (;) ni saltos de línea.");
        }
        
        // 4. El año de nacimiento debe ser mayor que 0
        if (nacimiento <= 0) {
            throw new IllegalArgumentException("El año de nacimiento debe ser mayor que 0.");
        }
        
        // 5. El año de fallecimiento, si existe, no puede ser menor que el de nacimiento
        if (fallecimiento != null && fallecimiento < nacimiento) {
            throw new IllegalArgumentException("El año de fallecimiento no puede ser menor al de nacimiento.");
        }
    }

    public String getPosicion() { return posicion; }
    public String getNombre() { return nombre; }
    public int getNacimiento() { return nacimiento; }
    public Integer getFallecimiento() { return fallecimiento; }

    @Override
    public String toString() {
        return posicion + " - " + nombre + " (" + nacimiento + "-" + (fallecimiento == null ? "" : fallecimiento) + ")";
    }
}