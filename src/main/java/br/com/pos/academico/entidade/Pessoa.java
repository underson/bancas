package br.com.pos.academico.entidade;

import java.text.ParseException;

import javax.swing.text.MaskFormatter;
import javax.validation.ValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.validator.routines.EmailValidator;

import br.com.pos.academico.constante.Graduacao;
import br.com.pos.experimental.Commons;

@EqualsAndHashCode(of = "codigo")
public class Pessoa {
	
	@Getter @Setter
	private String codigo;
	
	@Getter @Setter
	private Graduacao graduacao;
	
	@Getter
	private String email;
	
	@Getter
	private String nome;
	
	public Pessoa() {

	}

	public void setNome(String nome) {
		this.nome = Commons.normalizarNomeProprio(nome);
	}
	
	public String getCPF() {
		try {
			MaskFormatter mask = new MaskFormatter("###.###.###-##");
			mask.setValueContainsLiteralCharacters(false);
			return mask.valueToString(codigo);
		} catch (ParseException exception) {
			return codigo;
		}
	}

	public void setEmail(String email) {
		EmailValidator validator = EmailValidator.getInstance();
		if (!validator.isValid(email)) {
			throw new ValidationException("email.invalido");
		}
		
		this.email = email;
	}

}
