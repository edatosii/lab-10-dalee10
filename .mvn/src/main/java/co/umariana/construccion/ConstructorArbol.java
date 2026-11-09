package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class ItemPersona {
        Persona persona;
        int linea;

        ItemPersona(Persona persona, int linea) {
            this.persona = persona;
            this.linea = linea;
        }
    }

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío o no contiene líneas.");
        }

        String[] lineas = texto.split("\\r?\\n");
        if (lineas.length == 0) {
            throw new ArbolInvalidoException("El texto está vacío o no contiene líneas.");
        }

        List<ItemPersona> items = new ArrayList<>();
        Set<String> posicionesVistas = new HashSet<>();
        Persona sujeto = null;
        boolean tieneContenidoReal = false;

        for (int i = 0; i < lineas.length; i++) {
            int numLinea = i + 1;
            String linea = lineas[i];

            if (linea.trim().isEmpty()) {
                continue;
            }
            tieneContenidoReal = true;

            String[] campos = linea.split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": número de campos inválido.");
            }

            String posicion = campos[0].trim();
            String nombre = campos[1].trim();
            String nacimientoStr = campos[2].trim();
            String fallecimientoStr = (campos.length == 4) ? campos[3].trim() : "";

            int nacimiento;
            try {
                nacimiento = Integer.parseInt(nacimientoStr);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": año de nacimiento inválido.");
            }

            Integer fallecimiento = null;
            if (!fallecimientoStr.isEmpty()) {
                try {
                    fallecimiento = Integer.parseInt(fallecimientoStr);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException("Línea " + numLinea + ": año de fallecimiento inválido.");
                }
            }

            if (posicionesVistas.contains(posicion)) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": la posición " + posicion + " está repetida.");
            }

            Persona persona;
            try {
                persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
                persona.validar();
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage();
                if (msg != null && msg.startsWith("Línea")) {
                    throw new ArbolInvalidoException(msg);
                }
                throw new ArbolInvalidoException("Línea " + numLinea + ": " + msg);
            }

            posicionesVistas.add(posicion);
            if (Posicion.esSujeto(posicion)) {
                sujeto = persona;
            } else {
                items.add(new ItemPersona(persona, numLinea));
            }
        }

        if (!tieneContenidoReal) {
            throw new ArbolInvalidoException("El texto está vacío o no contiene líneas válidas.");
        }

        if (sujeto == null) {
            throw new ArbolInvalidoException("No se encontró el sujeto (posición 1).");
        }

        // Ordenar los ítems para procesar primero los ascendientes (por número Ahnentafel)
        // y luego los descendientes (por profundidad/generación)
        items.sort((item1, item2) -> {
            String pos1 = item1.persona.getPosicion();
            String pos2 = item2.persona.getPosicion();

            boolean esAn1 = Posicion.esAhnentafel(pos1);
            boolean esAn2 = Posicion.esAhnentafel(pos2);

            if (esAn1 && esAn2) {
                return Integer.compare(Integer.parseInt(pos1), Integer.parseInt(pos2));
            } else if (!esAn1 && !esAn2) {
                String[] partes1 = pos1.substring(2).split("\\.");
                String[] partes2 = pos2.substring(2).split("\\.");
                int minLen = Math.min(partes1.length, partes2.length);
                for (int k = 0; k < minLen; k++) {
                    int v1 = Integer.parseInt(partes1[k]);
                    int v2 = Integer.parseInt(partes2[k]);
                    if (v1 != v2) {
                        return Integer.compare(v1, v2);
                    }
                }
                return Integer.compare(partes1.length, partes2.length);
            } else {
                return esAn1 ? -1 : 1;
            }
        });

        ArbolGenealogico arbol = new ArbolGenealogico(sujeto);
        for (ItemPersona item : items) {
            try {
                arbol.agregar(item.persona);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && msg.startsWith("Línea")) {
                    throw new ArbolInvalidoException(msg);
                }
                throw new ArbolInvalidoException("Línea " + item.linea + ": " + (msg != null ? msg : e.toString()));
            }
        }

        return arbol;
    }
}