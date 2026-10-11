package cl.duoc.bancoxyz.clientes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    public record Cliente(Long id, String nombre, int edad, String email){}
    public record ClienteRequest(@NotBlank String nombre,@Min(18) @Max(99) int edad,String email){}
    private final JdbcTemplate db;
    public ClienteController(JdbcTemplate db){this.db=db;}
    @GetMapping public List<Cliente> listar(){return db.query("SELECT id,nombre,edad,email FROM clientes ORDER BY id",
        (rs,n)->new Cliente(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getString(4)));}
    @GetMapping("/{id}") public Cliente obtener(@PathVariable long id){
        return db.query("SELECT id,nombre,edad,email FROM clientes WHERE id=?",
            (rs,n)->new Cliente(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getString(4)),id)
            .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Cliente crear(@Valid @RequestBody ClienteRequest r){
        org.springframework.jdbc.support.KeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection -> {
            var ps=connection.prepareStatement("INSERT INTO clientes(nombre,edad,email) VALUES(?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,r.nombre());ps.setInt(2,r.edad());ps.setString(3,r.email());return ps;
        },key);
        return obtener(key.getKey().longValue());
    }
    @PutMapping("/{id}") public Cliente modificar(@PathVariable long id,@Valid @RequestBody ClienteRequest r){
        if(db.update("UPDATE clientes SET nombre=?, edad=?, email=? WHERE id=?",r.nombre(),r.edad(),r.email(),id)==0)
           throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no encontrado");
        return obtener(id);
    }
}
