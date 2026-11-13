package co.umariana.modelo;

public final class Posicion {

    public static final String SUJETO = "1";
    public static final int MAX_AHNENTAFEL = 2047;
    public static final int MAX_ORDEN = 9999;
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    public static boolean esAhnentafel(String posicion) {
        if (posicion == null || !posicion.matches("^[1-9]\\d*$")) {
            return false;
        }
        try {
            int n = Integer.parseInt(posicion);
            return n >= 1 && n <= MAX_AHNENTAFEL;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean esDAboville(String posicion) {
        if (posicion == null || posicion.length() > MAX_LONGITUD) {
            return false;
        }
        if (!posicion.startsWith("1.")) {
            return false;
        }
        // Se valida que no termine en punto
        if (posicion.endsWith(".")) {
            return false;
        }
        String[] partes = posicion.substring(2).split("\\.");
        if (partes.length == 0) {
            return false;
        }
        for (String parte : partes) {
            if (!parte.matches("^[1-9]\\d*$")) {
                return false;
            }
            try {
                int k = Integer.parseInt(parte);
                if (k < 1 || k > MAX_ORDEN) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    public static String posicionDelPadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("No es una posición Ahnentafel válida: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) * 2);
    }

    public static String posicionDeLaMadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("No es una posición Ahnentafel válida: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) * 2 + 1);
    }

    public static String posicionDelHijoEnLinea(String ahnentafel) {
        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException("No es un ascendiente: " + ahnentafel);
        }
        return String.valueOf(Integer.parseInt(ahnentafel) / 2);
    }

    public static String posicionDelProgenitor(String daboville) {
        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException("No es un descendiente válido: " + daboville);
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return daboville.substring(0, ultimoPunto);
    }

    public static int orden(String daboville) {
        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException("No es un descendiente válido: " + daboville);
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return Integer.parseInt(daboville.substring(ultimoPunto + 1));
    }

    public static int generacion(String posicion) {
        if (esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            // Distancia es la parte entera del logaritmo en base 2
            return (int) (Math.log(n) / Math.log(2));
        } else if (esDescendiente(posicion)) {
            // La distancia es la cantidad de puntos en la posición
            int count = 0;
            for (int i = 0; i < posicion.length(); i++) {
                if (posicion.charAt(i) == '.') {
                    count++;
                }
            }
            return count;
        }
        throw new IllegalArgumentException("Posición no válida: " + posicion);
    }
}