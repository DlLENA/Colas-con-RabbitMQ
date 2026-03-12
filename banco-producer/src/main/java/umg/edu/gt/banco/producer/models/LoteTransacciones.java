package umg.edu.gt.banco.producer.models;

import java.util.List;

public class LoteTransacciones {
    private String loteId;
    private String fechaGeneracion;
    private List<Transaccion> transacciones;

    public LoteTransacciones() {}

    public String getLoteId() { return loteId; }
    public void setLoteId(String loteId) { this.loteId = loteId; }

    public String getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(String fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public List<Transaccion> getTransacciones() { return transacciones; }
    public void setTransacciones(List<Transaccion> transacciones) { this.transacciones = transacciones; }
}
