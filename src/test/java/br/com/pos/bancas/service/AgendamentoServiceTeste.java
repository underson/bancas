package br.com.pos.bancas.service;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.com.pos.academico.constante.Situacao;
import br.com.pos.academico.entidade.Local;
import br.com.pos.banca.dao.AgendamentoDao;
import br.com.pos.banca.entidade.Agendamento;
import br.com.pos.banca.entidade.AgendamentoDia;
import br.com.pos.banca.entidade.AgendamentoHorario;
import br.com.pos.banca.entidade.AgendamentoSala;
import br.com.pos.persistencia.Paginacao;

import com.sun.net.httpserver.HttpServer;

public class AgendamentoServiceTeste {
	
	private Client cliente;
	private EntityManager manager;
	private EntityManagerFactory factory;
	
	private HttpServer server;
	private HttpContextBuilder builder;
	
	@Before
	public void iniciar() throws Exception {
		factory = Persistence.createEntityManagerFactory("bancas");
		manager = factory.createEntityManager();
		cliente = ClientBuilder.newClient();
		
		int porta = TestPortProvider.getPort();
		
		server = HttpServer.create(new InetSocketAddress(porta), 10);
		ResourceFactory resourceFactory = new AgendamentoServiceFactory(manager);

		builder = new HttpContextBuilder();
		builder.bind(server);

		builder.getDeployment().getRegistry().addResourceFactory(resourceFactory);
		server.start();
	}

	@After
	public void terminar() throws Exception {
		cliente.close(); 
		manager.close();
		factory.close();
		
		builder.cleanup();
		server.stop(0);
	}
	
	private Agendamento instanciarAgendamento() {

		AgendamentoHorario horario = new AgendamentoHorario();

		horario.setSituacao(Situacao.ATIVO);
		horario.setInicio(Calendar.getInstance());
		horario.setTermino(Calendar.getInstance());

		AgendamentoDia dia = new AgendamentoDia();
		dia.setDia(Calendar.getInstance());

		Local local = new Local();
		local.setNome("Local Teste");
		AgendamentoSala sala = new AgendamentoSala();
		sala.setSala(local);

		Agendamento agendamento = new Agendamento();		
		agendamento.setDia(dia);
		agendamento.setHorario(horario);
		agendamento.setSala(sala);		

		return agendamento;
	}

	@Test
	public void persistir() throws Exception {
		Collection<Agendamento> agendamentos;
		AgendamentoDao agendamentoDao = new AgendamentoDao(manager);
		agendamentos = agendamentoDao.buscar(new Agendamento(), new Paginacao());
		
		assertThat(agendamentos.isEmpty(), is(true));
		Agendamento agendamento = instanciarAgendamento();
		
		Agendamento response = cliente.target(TestPortProvider.generateURL("/agendamento")).path("/persistir").request().post(Entity.entity(agendamento, MediaType.APPLICATION_JSON), Agendamento.class);
		assertThat(response.getSala().getSala().getNome(), is("Local Teste"));
		
		agendamentos = agendamentoDao.buscar(new Agendamento(), new Paginacao());
		assertEquals(1, agendamentos.size());
	}

	@Test
	public void alterar() throws Exception {
		Agendamento agendamento = instanciarAgendamento();
		
		AgendamentoDao agendamentoDao = new AgendamentoDao(manager);
		agendamentoDao.persistir(agendamento);
		
		Agendamento recuperado = agendamentoDao.obter(agendamento.getCodigo());
		assertEquals("Local Teste", recuperado.getSala().getSala().getNome());
		
		agendamento.getSala().getSala().setNome("NOVO Local Teste");
		
		Agendamento response = cliente.target(TestPortProvider.generateURL("/agendamento")).path("/alterar").request().put(Entity.entity(agendamento, MediaType.APPLICATION_JSON), Agendamento.class);
		assertThat(response.getSala().getSala().getNome(), is("NOVO Local Teste"));
		
		recuperado = agendamentoDao.obter(agendamento.getCodigo());
		assertEquals("NOVO Local Teste", recuperado.getSala().getSala().getNome());
	}

	@Test
	public void excluir() throws Exception {
		Agendamento agendamento = instanciarAgendamento();
		AgendamentoDao agendamentoDao = new AgendamentoDao(manager);
		agendamentoDao.persistir(agendamento);
		Collection<Agendamento> agendamentos;
		
		agendamentos = agendamentoDao.buscar(new Agendamento(), new Paginacao());
		assertThat(agendamentos.isEmpty(), is(false));
		
		Integer codigo = agendamento.getCodigo();
		Agendamento response = cliente.target(TestPortProvider.generateURL("/agendamento")).path("/excluir/{codigo}").resolveTemplate("codigo", codigo).request().delete(Agendamento.class);
		
		agendamentos = agendamentoDao.buscar(new Agendamento(), new Paginacao());
		assertThat(response.getSala().getSala().getNome(), is("Teste"));
		assertThat(agendamentos.isEmpty(), is(true));
	}
	
	@Test
	@SuppressWarnings("all")
	public void listar() throws Exception {
		Collection<Agendamento> agendamentos;
		AgendamentoDao agendamentoDao = new AgendamentoDao(manager);
		agendamentos = agendamentoDao.buscar(new Agendamento(), new Paginacao());
		
		assertThat(agendamentos.isEmpty(), is(true));
		
		Agendamento agendamento = instanciarAgendamento();
		agendamentoDao.persistir(agendamento);
		
		agendamentos = cliente.target(TestPortProvider.generateURL("/agendamento")).path("/listar").request().get(Collection.class);
		assertEquals(1, agendamentos.size());
	}

	@Test
	@SuppressWarnings("all")
	public void buscar() throws Exception {
		Agendamento agendamento = instanciarAgendamento();
		AgendamentoDao agendamentoDao = new AgendamentoDao(manager);
		agendamentoDao.persistir(agendamento);
		
		Agendamento exemplo = new Agendamento();
		exemplo.setSala(new AgendamentoSala());
		exemplo.getSala().setSala(new Local());
		exemplo.getSala().getSala().setNome("Teste - NOME LOCAL \\o//");
		
		Collection<Agendamento> response = cliente.target(TestPortProvider.generateURL("/agendamento")).path("/buscar").request().post(Entity.entity(agendamento, MediaType.APPLICATION_JSON), Collection.class);
		
		Collection<Agendamento> agendamentos = agendamentoDao.buscar(exemplo, new Paginacao());
		assertThat(agendamentos.isEmpty(), is(true));
		assertEquals(0, response.size());
	}

}
