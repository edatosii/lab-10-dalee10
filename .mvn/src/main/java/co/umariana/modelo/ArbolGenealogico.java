package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre;
        Nodo madre;
        List<Nodo> hijos;

        Nodo(Persona persona) {
            this.persona = persona;
            this.padre = null;
            this.madre = null;
            this.hijos = new ArrayList<>();
        }
    }

    private Nodo raiz;
    private int cantidadMiembros;

    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser nulo");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El árbol debe iniciar con el sujeto en la posición 1");
        }
        this.raiz = new Nodo(sujeto);
        this.cantidadMiembros = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidadMiembros;
    }

    private Nodo buscarNodo(String posicion) {
        if (posicion == null || !Posicion.esValida(posicion)) {
            return null;
        }
        if (Posicion.esSujeto(posicion)) {
            return raiz;
        }

        Nodo actual = raiz;

        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            String ruta = "";
            int temp = n;
            while (temp > 1) {
                ruta = (temp % 2 == 0 ? "P" : "M") + ruta;
                temp = temp / 2;
            }

            for (int i = 0; i < ruta.length(); i++) {
                char paso = ruta.charAt(i);
                if (paso == 'P') {
                    if (actual == null) return null;
                    actual = actual.padre;
                } else {
                    if (actual == null) return null;
                    actual = actual.madre;
                }
            }
            return actual;

        } else if (Posicion.esDescendiente(posicion)) {
            String[] partes = posicion.substring(2).split("\\.");
            for (String parte : partes) {
                int indiceHijo = Integer.parseInt(parte) - 1;
                if (actual == null || indiceHijo < 0 || indiceHijo >= actual.hijos.size()) {
                    return null;
                }
                actual = actual.hijos.get(indiceHijo);
                if (actual == null) return null;
            }
            return actual;
        }

        return null;
    }

    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo != null ? nodo.persona : null;
    }

    public boolean contiene(String posicion) {
        return buscar(posicion) != null;
    }

    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula");
        }
        persona.validar();
        String pos = persona.getPosicion();
        
        if (contiene(pos)) {
            throw new ArbolInvalidoException("La posición ya existe: " + pos);
        }
        if (Posicion.esSujeto(pos)) {
            throw new ArbolInvalidoException("El sujeto ya existe.");
        }

        if (Posicion.esAhnentafel(pos)) {
            int n = Integer.parseInt(pos);
            String posHijo = String.valueOf(n / 2);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo == null) {
                throw new ArbolInvalidoException("No existe el miembro del cual depende: " + posHijo);
            }
            if (persona.getNacimiento() >= nodoHijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su descendiente.");
            }

            Nodo nuevoNodo = new Nodo(persona);
            if (n % 2 == 0) {
                if (nodoHijo.padre != null) {
                    throw new ArbolInvalidoException("El padre ya existe.");
                }
                nodoHijo.padre = nuevoNodo;
            } else {
                if (nodoHijo.madre != null) {
                    throw new ArbolInvalidoException("La madre ya existe.");
                }
                nodoHijo.madre = nuevoNodo;
            }
            cantidadMiembros++;
            return persona;

        } else if (Posicion.esDescendiente(pos)) {
            String posProgenitor = Posicion.posicionDelProgenitor(pos);
            Nodo nodoProg = buscarNodo(posProgenitor);
            if (nodoProg == null) {
                throw new ArbolInvalidoException("No existe el progenitor: " + posProgenitor);
            }
            if (persona.getNacimiento() <= nodoProg.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después de su progenitor.");
            }

            int orden = Posicion.orden(pos);
            int indice = orden - 1;

            while (nodoProg.hijos.size() < indice) {
                nodoProg.hijos.add(null);
            }

            if (nodoProg.hijos.size() == indice) {
                nodoProg.hijos.add(new Nodo(persona));
            } else {
                if (nodoProg.hijos.get(indice) != null) {
                    throw new ArbolInvalidoException("La posición del hijo ya está ocupada.");
                }
                nodoProg.hijos.set(indice, new Nodo(persona));
            }
            cantidadMiembros++;
            return persona;
        }

        throw new ArbolInvalidoException("Posición no válida: " + pos);
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!contiene(posicion) || (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion))) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
        String posPadre = Posicion.posicionDelPadre(posicion);
        return agregar(new Persona(posPadre, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!contiene(posicion) || (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion))) {
            throw new ArbolInvalidoException("Solo se pueden agregar madres al sujeto y a sus ascendientes.");
        }
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        return agregar(new Persona(posMadre, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!contiene(posicion) || (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion))) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }
        Nodo nodoProg = buscarNodo(posicion);
        int siguienteOrden = nodoProg.hijos.size() + 1;
        String posHijo = posicion + "." + siguienteOrden;
        return agregar(new Persona(posHijo, nombre, nacimiento, fallecimiento));
    }

    private int contarYLimpiarSubarbol(Nodo nodo) {
        if (nodo == null) return 0;
        int count = 1;
        count += contarYLimpiarSubarbol(nodo.padre);
        count += contarYLimpiarSubarbol(nodo.madre);
        if (nodo.hijos != null) {
            for (Nodo h : nodo.hijos) {
                if (h != null) {
                    count += contarYLimpiarSubarbol(h);
                }
            }
        }
        return count;
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar.");
        }
        Nodo nodoEliminar = buscarNodo(posicion);
        if (nodoEliminar == null) {
            throw new ArbolInvalidoException("La posición no existe: " + posicion);
        }

        int eliminados = contarYLimpiarSubarbol(nodoEliminar);

        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            String posHijo = String.valueOf(n / 2);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo != null) {
                if (nodoHijo.padre == nodoEliminar) nodoHijo.padre = null;
                else if (nodoHijo.madre == nodoEliminar) nodoHijo.madre = null;
            }
        } else if (Posicion.esDescendiente(posicion)) {
            String posProg = Posicion.posicionDelProgenitor(posicion);
            Nodo nodoProg = buscarNodo(posProg);
            if (nodoProg != null && nodoProg.hijos != null) {
                int orden = Posicion.orden(posicion);
                int idx = orden - 1;
                if (idx >= 0 && idx < nodoProg.hijos.size()) {
                    nodoProg.hijos.set(idx, null);
                }
            }
        }

        cantidadMiembros -= eliminados;
        return eliminados;
    }

    private void recolectarAscendientes(Nodo nodo, List<Persona> lista) {
        if (nodo == null) return;
        if (nodo.padre != null) {
            lista.add(nodo.padre.persona);
            recolectarAscendientes(nodo.padre, lista);
        }
        if (nodo.madre != null) {
            lista.add(nodo.madre.persona);
            recolectarAscendientes(nodo.madre, lista);
        }
    }

    public List<Persona> ascendientes() {
        List<Persona> lista = new ArrayList<>();
        recolectarAscendientes(raiz, lista);
        lista.sort((p1, p2) -> Integer.compare(Integer.parseInt(p1.getPosicion()), Integer.parseInt(p2.getPosicion())));
        return lista;
    }

    private void recolectarPreordenDescendientes(Nodo nodo, List<Persona> lista) {
        if (nodo == null || nodo.hijos == null) return;
        for (Nodo hijo : nodo.hijos) {
            if (hijo != null) {
                lista.add(hijo.persona);
                recolectarPreordenDescendientes(hijo, lista);
            }
        }
    }

    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        recolectarPreordenDescendientes(raiz, lista);
        return lista;
    }

    public List<Persona> miembros() {
        List<Persona> todos = new ArrayList<>();
        todos.add(sujeto());
        todos.addAll(ascendientes());
        todos.addAll(descendientes());
        return todos;
    }
}