package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 100 || arbol == null) {
            return -1;
        }

        String sqlArbol = "INSERT INTO arboles (nombre) VALUES (?)";
        String sqlMiembro = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conectar()) {
            if (con == null) {
                return -1;
            }

            con.setAutoCommit(false);
            long arbolId = -1;

            try (PreparedStatement psArbol = con.prepareStatement(sqlArbol, Statement.RETURN_GENERATED_KEYS)) {
                psArbol.setString(1, nombre.trim());
                int filasAfectadas = psArbol.executeUpdate();
                if (filasAfectadas == 0) {
                    con.rollback();
                    con.setAutoCommit(true);
                    return -1;
                }

                try (ResultSet generatedKeys = psArbol.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        arbolId = generatedKeys.getLong(1);
                    } else {
                        con.rollback();
                        con.setAutoCommit(true);
                        return -1;
                    }
                }
            }

            try (PreparedStatement psMiembro = con.prepareStatement(sqlMiembro)) {
                for (Persona p : arbol.miembros()) {
                    psMiembro.setLong(1, arbolId);
                    psMiembro.setString(2, p.getPosicion());
                    psMiembro.setString(3, p.getNombre());
                    psMiembro.setInt(4, p.getNacimiento());
                    if (p.getFallecimiento() != null) {
                        psMiembro.setInt(5, p.getFallecimiento());
                    } else {
                        psMiembro.setNull(5, java.sql.Types.INTEGER);
                    }
                    psMiembro.addBatch();
                }
                psMiembro.executeBatch();
            }

            con.commit();
            con.setAutoCommit(true);
            return arbolId;

        } catch (SQLException e) {
            System.out.println("Error al guardar el árbol: " + e.getMessage());
            return -1;
        }
    }

    public List<ResumenArbol> listar() {
        List<ResumenArbol> lista = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total " +
                     "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id " +
                     "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";

        try (Connection con = ConexionBD.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String nombre = rs.getString("nombre");
                Timestamp timestamp = rs.getTimestamp("creado");
                java.time.LocalDateTime creado = (timestamp != null) ? timestamp.toLocalDateTime() : null;
                int totalMiembros = rs.getInt("total");

                lista.add(new ResumenArbol(id, nombre, totalMiembros, creado));
            }

        } catch (SQLException e) {
            System.out.println("Error al listar los árboles: " + e.getMessage());
        }
        return lista;
    }

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ? ORDER BY id";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;
                boolean primeraFila = true;

                while (rs.next()) {
                    String posicion = rs.getString("posicion");
                    String nombre = rs.getString("nombre");
                    int nacimiento = rs.getInt("nacimiento");
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;

                    Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);

                    if (primeraFila) {
                        if (!posicion.equals("1")) {
                            return null;
                        }
                        arbol = new ArbolGenealogico(persona);
                        primeraFila = false;
                    } else {
                        arbol.agregar(persona);
                    }
                }
                return arbol;
            }

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error al cargar el árbol: " + e.getMessage());
            return null;
        }
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }

        String sqlCheck = "SELECT id FROM arboles WHERE id = ?";
        String sqlDelete = "DELETE FROM miembros WHERE arbol_id = ?";
        String sqlInsert = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conectar()) {
            if (con == null) {
                return false;
            }

            con.setAutoCommit(false);

            try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                psCheck.setLong(1, id);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        con.setAutoCommit(true);
                        return false;
                    }
                }
            }

            try (PreparedStatement psDelete = con.prepareStatement(sqlDelete)) {
                psDelete.setLong(1, id);
                psDelete.executeUpdate();
            }

            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                for (Persona p : arbol.miembros()) {
                    psInsert.setLong(1, id);
                    psInsert.setString(2, p.getPosicion());
                    psInsert.setString(3, p.getNombre());
                    psInsert.setInt(4, p.getNacimiento());
                    if (p.getFallecimiento() != null) {
                        psInsert.setInt(5, p.getFallecimiento());
                    } else {
                        psInsert.setNull(5, java.sql.Types.INTEGER);
                    }
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
            }

            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar el árbol: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.out.println("Error al eliminar el árbol: " + e.getMessage());
            return false;
        }
    }
}