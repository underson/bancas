package br.com.pos.bancas.service;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.com.pos.academico.constante.Graduacao;
import br.com.pos.academico.constante.GrupoUsuario;
import br.com.pos.academico.constante.Situacao;
import br.com.pos.academico.entidade.Curso;
import br.com.pos.academico.entidade.Pessoa;
import br.com.pos.academico.entidade.Usuario;
import br.com.pos.banca.dao.TrabalhoDao;
import br.com.pos.banca.entidade.Trabalho;
import br.com.pos.persistencia.Paginacao;

public class TrabalhoServiceTeste {
	
	private Client cliente;
	private EntityManager manager;
	private NettyJaxrsServer server;
	private EntityManagerFactory factory;
	
	@Before
	public void iniciar() throws Exception {
		factory = Persistence.createEntityManagerFactory("bancas");
		manager = factory.createEntityManager();
		cliente = ClientBuilder.newClient();
		
		int porta = TestPortProvider.getPort();

		server = new NettyJaxrsServer();
		server.setRootResourcePath("");
		server.setSecurityDomain(null);
		server.setPort(porta);
		server.start();
		
		ResourceFactory resourceFactory = new TrabalhoServiceFactory(manager);
		server.getDeployment().getRegistry().addResourceFactory(resourceFactory);
	}

	@After
	public void terminar() throws Exception {
		cliente.close(); 
		manager.close();
		factory.close();
		server.stop();
	}
	
	private Trabalho instanciarTrabalho() {
		Trabalho trabalho;
		trabalho = new Trabalho();
		trabalho.setTitulo("Teste");
		trabalho.setResumo("Trabalho de Teste");
		
		Curso curso;
		curso = new Curso();
		curso.setSigla("SEG");
		curso.setCodigo("SEG");
		curso.setSituacao(Situacao.ATIVO);
		curso.setNome("Seguranca em Redes");
		
		trabalho.setCurso(curso);
		
		Pessoa docente;
		docente = new Pessoa();
		docente.setNome("Pesquisador");
		docente.setCodigo("00000000000");
		docente.setGraduacao(Graduacao.MESTRE);
		docente.setEmail("pesquisador@email.com");
		
		Usuario orientador;
		orientador = new Usuario();
		orientador.setSenha("senha");
		orientador.setPessoa(docente);
		orientador.setLogin("00000000000");
		orientador.setSituacao(Situacao.ATIVO);
		orientador.setGrupo(GrupoUsuario.DOCENTE);
		
		trabalho.setOrientador(orientador);
		
		Pessoa aluno;
		aluno = new Pessoa();
		aluno.setNome("Aluno");
		aluno.setCodigo("11111111111");
		aluno.setEmail("aluno@email.com");
		
		Usuario orientado;
		orientado = new Usuario();
		orientado.setPessoa(aluno);
		orientado.setSenha("senha");
		orientado.setLogin("11111111111");
		orientado.setSituacao(Situacao.ATIVO);
		orientado.setGrupo(GrupoUsuario.ALUNO);
		
		trabalho.adicionarAluno(orientado);
		return trabalho;
	}

	@Test
	public void persistir() throws Exception {
		Collection<Trabalho> trabalhos;
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		
		assertThat(trabalhos.isEmpty(), is(true));
		Trabalho trabalho = instanciarTrabalho();
		
		Trabalho response = cliente.target(TestPortProvider.generateURL("/trabalho")).path("/persistir").request().post(Entity.entity(trabalho, MediaType.APPLICATION_JSON), Trabalho.class);
		assertThat(response.getTitulo(), is("Teste"));
		
		trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		assertEquals(1, trabalhos.size());
	}

	@Test
	public void alterar() throws Exception {
		Trabalho trabalho = instanciarTrabalho();
		
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.persistir(trabalho);
		
		Trabalho recuperado = trabalhoDao.obter(trabalho.getCodigo());
		assertEquals("Teste", recuperado.getTitulo());
		
		trabalho.setTitulo("Novo Titulo");
		
		Trabalho response = cliente.target(TestPortProvider.generateURL("/trabalho")).path("/alterar").request().put(Entity.entity(trabalho, MediaType.APPLICATION_JSON), Trabalho.class);
		assertThat(response.getTitulo(), is("Novo Titulo"));
		
		recuperado = trabalhoDao.obter(trabalho.getCodigo());
		assertEquals("Novo Titulo", recuperado.getTitulo());
	}

	@Test
	public void excluir() throws Exception {
		Trabalho trabalho = instanciarTrabalho();
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.persistir(trabalho);
		Collection<Trabalho> trabalhos;
		
		trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		assertThat(trabalhos.isEmpty(), is(false));
		
		Integer codigo = trabalho.getCodigo();
		Trabalho response = cliente.target(TestPortProvider.generateURL("/trabalho")).path("/excluir/{codigo}").resolveTemplate("codigo", codigo).request().delete(Trabalho.class);
		
		trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		assertThat(response.getTitulo(), is("Teste"));
		assertThat(trabalhos.isEmpty(), is(true));
	}

	@Test
	@SuppressWarnings("all")
	public void listar() throws Exception {
		Collection<Trabalho> trabalhos;
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhos = trabalhoDao.buscar(new Trabalho(), new Paginacao());
		
		assertThat(trabalhos.isEmpty(), is(true));
		
		Trabalho trabalho = instanciarTrabalho();
		trabalhoDao.persistir(trabalho);
		
		trabalhos = cliente.target(TestPortProvider.generateURL("/trabalho")).path("/listar").request().get(Collection.class);
		assertEquals(1, trabalhos.size());
	}

	@Test
	@SuppressWarnings("all")
	public void buscar() throws Exception {
		Trabalho trabalho = instanciarTrabalho();
		TrabalhoDao trabalhoDao = new TrabalhoDao(manager);
		trabalhoDao.persistir(trabalho);
		
		Trabalho exemplo = new Trabalho();
		exemplo.setTitulo("Teste");
		
		Collection<Trabalho> response = cliente.target(TestPortProvider.generateURL("/trabalho")).path("/buscar").request().post(Entity.entity(trabalho, MediaType.APPLICATION_JSON), Collection.class);
		
		Collection<Trabalho> trabalhos = trabalhoDao.buscar(exemplo, new Paginacao());
		assertThat(trabalhos.isEmpty(), is(false));
		assertEquals(1, response.size());
	}

}
