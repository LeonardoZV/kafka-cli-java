package br.com.leonardozv.kafka.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

import br.com.leonardozv.kafka.cli.config.AppConfiguration;
import br.com.leonardozv.kafka.cli.services.GerarPostarEventoService;
import br.com.leonardozv.kafka.cli.services.KafkaConsumerService;

@SpringBootApplication
public class KafkaCliApplication implements CommandLineRunner {
	
	private static final Logger log = LoggerFactory.getLogger(KafkaCliApplication.class);

	private final AppConfiguration appConfiguration;

	private final KafkaConsumerService kafkaConsumerService;

	private final GerarPostarEventoService gerarPostarEventoService;

	@Autowired
	public KafkaCliApplication(AppConfiguration appConfiguration, KafkaConsumerService kafkaConsumerService, GerarPostarEventoService gerarPostarEventoService) {
		this.appConfiguration = appConfiguration;
		this.kafkaConsumerService = kafkaConsumerService;
		this.gerarPostarEventoService = gerarPostarEventoService;
	}

	public static void main(String[] args) {

		SpringApplication.run(KafkaCliApplication.class, args);

	}
	
    @Override
    public void run(String... args) throws Exception {

    	if (this.appConfiguration.getAction() == null) {
    		log.error("Parâmetro 'action' não informado.");
    		return;
    	}
    	
    	if (!this.appConfiguration.getAction().equals("consume") && !this.appConfiguration.getAction().equals("produce")) {
    		log.error("Parâmetro 'action' inválido.");
    		return;
    	}

		if (this.appConfiguration.getAction().equals("consume")) {
			consumeAction();
		}


    	if (this.appConfiguration.getAction().equals("produce")) {
			produceAction();
    	}
    	
    }

	private void consumeAction() {

		if (this.appConfiguration.getTopics()[0].equals("default")) {
			log.error("Parâmetro 'topics' não informado.");
		}

		this.kafkaConsumerService.start();

	}

	private void produceAction() throws IOException, InterruptedException {

		if (this.appConfiguration.getTopic().equals("default")) {
			log.error("Parâmetro 'topic' não informado.");
			return;
		}

		String topico = this.appConfiguration.getTopic();

		if (this.appConfiguration.getSchema().equals("default")) {
			log.error("Parâmetro 'schema' não informado.");
			return;
		}

		Schema schema = new Schema.Parser().parse(Files.readString(Paths.get(this.appConfiguration.getApplicationSchemaFolderLocation() + this.appConfiguration.getSchema() + ".avsc")));

		String header = null;

		if (Boolean.TRUE.equals(this.appConfiguration.getHeader())) {

			header = Files.readString(Paths.get(this.appConfiguration.getApplicationHeaderFolderLocation() + this.appConfiguration.getSchema() + ".json"));

		}

		String key = null;

		if (Boolean.TRUE.equals(this.appConfiguration.getKey())) {

			key = Files.readString(Paths.get(this.appConfiguration.getApplicationKeyFolderLocation() + this.appConfiguration.getSchema() + ".json"));

		}

		String payload = Files.readString(Paths.get(this.appConfiguration.getApplicationPayloadFolderLocation() + this.appConfiguration.getSchema() + ".json"));

		StopWatch sw = new StopWatch();

		sw.start();

		for (int b = 1; b <= this.appConfiguration.getBatches(); b++) {

			this.gerarPostarEventoService.gerarPostarEvento(topico, schema, header, key, payload);

		}

		sw.stop();

		long qtdEventosPostados = this.appConfiguration.getBatches() * this.appConfiguration.getEvents();

		double qtdEventosPostadosPorSegundo = qtdEventosPostados / sw.getTotalTimeSeconds();

		log.info("Foram postados {} eventos em {} segundos, ficando {} evt/s.", qtdEventosPostados, sw.getTotalTimeSeconds(), qtdEventosPostadosPorSegundo);

	}

}