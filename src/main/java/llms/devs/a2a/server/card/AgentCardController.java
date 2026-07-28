package llms.devs.a2a.server.card;

import org.a2aproject.sdk.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentCardController {

	private static final Logger logger = LoggerFactory.getLogger(AgentCardController.class);

	private final AgentCard agentCard;

	public AgentCardController(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	@GetMapping(path = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public AgentCard getAgentCard() {
		return this.agentCard;
	}
}
