package br.com.pos.banca.service;

import java.util.Collection;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.pos.banca.dao.TrabalhoDao;
import br.com.pos.banca.entidade.Trabalho;
import br.com.pos.persistencia.Paginacao;

@Path("trabalho")
public class TrabalhoService  {
	
	@Inject
	private EntityManager manager;
	
	public TrabalhoService() {

	}
	
	@GET
	public Response buscar() throws Exception {
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		Collection<Trabalho> trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		return Response.ok(trabalhos, MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	public Response persistir(Trabalho trabalho) throws Exception {
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.persistir(trabalho);

		return Response.ok(trabalho, MediaType.APPLICATION_JSON).build();
	}

	@PUT
	public Response alterar(Trabalho trabalho) throws Exception {
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.atualizar(trabalho);

		return Response.ok(trabalho, MediaType.APPLICATION_JSON).build();
	}

	@DELETE
	@Path("{codigo}")
	public Response excluir(@PathParam("codigo") Integer codigo) throws Exception {
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.remover(trabalhoDao.obter(codigo));

		return Response.ok(trabalhoDao.buscar(new Trabalho(),
		new Paginacao()), MediaType.APPLICATION_JSON).build();
	}

}
