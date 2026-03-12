package umg.edu.gt.banco.consumer.models;

public class Transaccion {
    private String idTransaccion;
    private double monto;
    private String moneda;
    private String cuentaOrigen;
    private String bancoDestino;
    private Detalle detalle;
    
    private String nombre; 
    private String carnet; 
    private String correo;

    public Transaccion() {}

    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCarnet() { return carnet; }
    public void setCarnet(String carnet) { this.carnet = carnet; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public String getCuentaOrigen() { return cuentaOrigen; }
    public void setCuentaOrigen(String cuentaOrigen) { this.cuentaOrigen = cuentaOrigen; }

    public String getBancoDestino() { return bancoDestino; }
    public void setBancoDestino(String bancoDestino) { this.bancoDestino = bancoDestino; }

    public Detalle getDetalle() { return detalle; }
    public void setDetalle(Detalle detalle) { this.detalle = detalle; }
}

