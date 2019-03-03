package lighthouse.ui.board.view.discord;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lighthouse.model.Board;
import lighthouse.ui.board.view.LighthouseView;
import lighthouse.ui.board.viewmodel.LighthouseViewModel;
import lighthouse.util.Listener;
import lighthouse.util.ListenerList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

/**
 * A Lighthouse view that uses the Discord
 * API to present the current Lighthouse state.
 */
public class DiscordLighthouseView implements LighthouseView {
	private static final Logger LOG = LoggerFactory.getLogger(DiscordLighthouseView.class);
	
	private final Map<String, DiscordCommand> commands = new HashMap<>();
	private final Set<MessageChannel> activeChannels = new HashSet<>();
	private final ListenerList<Void> readyListeners = new ListenerList<>("DiscordLighthouseView.readyListeners");
	private final BufferedImage boardImage;
	private Board lastBoard = null;
	
	private final Pattern commandPattern;
	private JDA jda;
	
	public DiscordLighthouseView(String prefix, int imageWidth, int imageHeight) {
		commandPattern = Pattern.compile(Pattern.quote(prefix) + "(\\w+)(?:\\s+(.+))?");
		boardImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		registerCommands();
	}
	
	private void registerCommands() {
		commands.put("ping", new PingCommand());
		commands.put("summon", new SummonCommand(activeChannels));
		commands.put("unsummon", new UnsummonCommand(activeChannels));
		commands.put("help", this::showHelp);
	}
	
	public void connect(String token) {
		try {
			jda = new JDABuilder(token)
				.addEventListeners((EventListener) this::onEvent)
				.build();
		} catch (LoginException e) {
			LOG.error("Error while connecting to discord:", e);
		}
	}
	
	private void onEvent(GenericEvent event) {
		if (event instanceof ReadyEvent) {
			readyListeners.fire();
		} else if (event instanceof MessageReceivedEvent) {
			onMessage((MessageReceivedEvent) event);
		}
	}
	
	private void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String text = msg.getContentStripped();
		Matcher matcher = commandPattern.matcher(text);
		
		if (matcher.find()) {
			String command = matcher.group(1);
			String args = matcher.group(2);
			
			if (commands.containsKey(command)) {
				commands.get(command).invoke(args, msg.getAuthor(), msg.getChannel());
			} else {
				msg.getChannel().sendMessage("Sorry, I did not recognize the command `" + command + "`").queue();
			}
		} else {
			LOG.debug("Received non-command message for Discord: {}", msg);
		}
	}
	
	private void showHelp(String args, User author, MessageChannel channel) {
		StringBuilder text = new StringBuilder("Available commands:\n```\n");
		
		for (String command : commands.keySet()) {
			text.append(command).append('\n');
		}
		
		channel.sendMessage(text.append("```")).queue();
	}
	
	public boolean isConnected() {
		return jda != null && jda.getStatus().equals(JDA.Status.CONNECTED);
	}
	
	public void addReadyListener(Listener<Void> listener) {
		readyListeners.add(listener);
	}
	
	public void removeReadyListener(Listener<Void> listener) {
		readyListeners.remove(listener);
	}
	
	@Override
	public void draw(LighthouseViewModel viewModel) {
		if (isConnected()) {
			viewModel.render();
			Board board = viewModel.getBoard().getModel();
			
			if (lastBoard == null || !board.equals(lastBoard)) {
				// Only draw if the board has changed
				
				Graphics2D g2d = boardImage.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g2d.drawImage(viewModel.getImage(), 0, 0, boardImage.getWidth(), boardImage.getHeight(), null);
				g2d.dispose();
				
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(boardImage, "png", baos);
					byte[] imgBytes = baos.toByteArray();
					
					for (MessageChannel channel : activeChannels) {
						channel.sendFile(imgBytes, "lighthouse.png").queue();
					}
				} catch (IOException e) {
					LOG.error("Error while sending image to Discord:", e);
				}
				
				lastBoard = board.copy();
			}
		}
	}
}
