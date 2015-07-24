package burlap.behavior.singleagent.learning.modellearning.artdp;

import java.util.LinkedList;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.policy.Policy;
import burlap.behavior.valuefunction.QValue;
import burlap.behavior.valuefunction.ValueFunctionInitialization;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.modellearning.Model;
import burlap.behavior.singleagent.learning.modellearning.ModeledDomainGenerator;
import burlap.behavior.singleagent.learning.modellearning.models.TabularModel;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.policy.SolverDerivedPolicy;
import burlap.behavior.valuefunction.QFunction;
import burlap.behavior.valuefunction.ValueFunctionPlanner;
import burlap.behavior.policy.BoltzmannQPolicy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.AbstractGroundedAction;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.environment.Environment;
import burlap.oomdp.singleagent.environment.EnvironmentOutcome;


/**
 * This class provides an implementation of Adaptive Realtime Dynamic Programming [1]. By default, a tabular model will be used and a boltzmann distribution with
 * a temperature of 0.1 will be used. A different model can be provided in the constructor as well as the value function initialization used. The policy
 * followed may be set with a setter ({@link #setPolicy(burlap.behavior.policy.SolverDerivedPolicy)}). The Q-value assigned to state-action pairs for entirely untried
 * transitions is reported as that returned by the value function initializer provided. In general, value function initialization should always be optimistic.
 * 
 * 
 * 1. Barto, Andrew G., Steven J. Bradtke, and Satinder P. Singh. "Learning to act using real-time dynamic programming." Artificial Intelligence 72.1 (1995): 81-138.
 * 
 * @author James MacGlashan
 *
 */
public class ARTDP extends MDPSolver implements QFunction,LearningAgent {

	/**
	 * The model of the world that is being learned.
	 */
	protected Model								model;
	
	/**
	 * The planner used on the modeled world to update the value function
	 */
	protected ValueFunctionPlanner				modelPlanner;
	
	/**
	 * the policy to follow
	 */
	protected Policy							policy;
	
	/**
	 * the saved previous learning episodes
	 */
	protected LinkedList<EpisodeAnalysis>		episodeHistory = new LinkedList<EpisodeAnalysis>();
	
	
	/**
	 * The maximum number of learning steps per episode before the agent gives up
	 */
	protected int								maxNumSteps = Integer.MAX_VALUE;
	
	
	/**
	 * The number of the most recent learning episodes to store.
	 */
	protected int								numEpisodesToStore = 1;
	
	
	
	
	/**
	 * Initializes using a tabular model of the world and a Boltzmann policy with a fixed temperature of 0.1. 
	 * @param domain the domain
	 * @param rf the reward function
	 * @param tf the termianl function
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory to use for the tabular model and the planning
	 * @param vInit the constant value function initialization to use; should be optimisitc.
	 */
	public ARTDP(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double vInit){
		
		this.solverInit(domain, rf, tf, gamma, hashingFactory);
		
		this.model = new TabularModel(domain, hashingFactory, 1);
		ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, this.model, true);
		
		//initializing the value function planning mechanisms to use our model and not the real world
		this.modelPlanner = new ARTDPPlanner(mdg.generateDomain(), this.model.getModelRF(), this.model.getModelTF(), gamma, hashingFactory, vInit);
		this.policy = new BoltzmannQPolicy(this, 0.1);
		
		
	}
	
	
	/**
	 * Initializes using a tabular model of the world and a Boltzmann policy with a fixed temperature of 0.1. 
	 * @param domain the domain
	 * @param rf the reward function
	 * @param tf the termianl function
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory to use for the tabular model and the planning
	 * @param vInit the value function initialization to use; should be optimisitc.
	 */
	public ARTDP(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, ValueFunctionInitialization vInit){
		
		this.solverInit(domain, rf, tf, gamma, hashingFactory);
		
		this.model = new TabularModel(domain, hashingFactory, 1);
		ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, this.model, true);
		
		//initializing the value function planning mechanisms to use our model and not the real world
		this.modelPlanner = new ARTDPPlanner(mdg.generateDomain(), this.model.getModelRF(), this.model.getModelTF(), gamma, hashingFactory, vInit);
		this.policy = new BoltzmannQPolicy(this, 0.1);
		
		
	}
	
	
	/**
	 * Initializes using the provided model algorithm and a Boltzmann policy with a fixed temperature of 0.1. 
	 * @param domain the domain
	 * @param rf the reward function
	 * @param tf the termianl function
	 * @param gamma the discount factor
	 * @param hashingFactory the state hashing factory to use for the tabular model and the planning
	 * @param model the model algorithm to use
	 * @param vInit the constant value function initialization to use; should be optimisitc.
	 */
	public ARTDP(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, Model model, ValueFunctionInitialization vInit){
		
		this.solverInit(domain, rf, tf, gamma, hashingFactory);
		
		this.model = model;
		ModeledDomainGenerator mdg = new ModeledDomainGenerator(domain, this.model, true);
		
		//initializing the value function planning mechanisms to use our model and not the real world
		this.modelPlanner = new ARTDPPlanner(mdg.generateDomain(), this.model.getModelRF(), this.model.getModelTF(), gamma, hashingFactory, vInit);
		this.policy = new BoltzmannQPolicy(this, 0.1);
		
		
	}
	
	/**
	 * Sets the policy to the provided one. Should be a policy that operates on a {@link burlap.behavior.valuefunction.QFunction}. Will automatically set its
	 * Q-source to this object.
	 * @param policy the policy to use.
	 */
	public void setPolicy(SolverDerivedPolicy policy){
		this.policy = (Policy)policy;
		policy.setSolver(this);
		
	}


	@Override
	public EpisodeAnalysis runLearningEpisode(Environment env) {
		return this.runLearningEpisode(env, -1);
	}

	@Override
	public EpisodeAnalysis runLearningEpisode(Environment env, int maxSteps) {

		State initialState = env.getCurState();

		EpisodeAnalysis ea = new EpisodeAnalysis(initialState);

		State curState = initialState;
		int steps = 0;
		while(!env.curStateIsTerminal() && (steps < maxSteps || maxSteps == -1)){
			GroundedAction ga = (GroundedAction)policy.getAction(curState);
			EnvironmentOutcome eo = ga.executeIn(env);


			ea.recordTransitionTo(ga, eo.sp, eo.r);

			this.model.updateModel(eo);

			this.modelPlanner.performBellmanUpdateOn(eo.s);

			curState = env.getCurState();
			steps++;

		}

		return ea;
	}



	public EpisodeAnalysis getLastLearningEpisode() {
		return episodeHistory.getLast();
	}

	public void setNumEpisodesToStore(int numEps) {
		if(numEps > 0){
			numEpisodesToStore = numEps;
		}
		else{
			numEpisodesToStore = 1;
		}
	}

	public List<EpisodeAnalysis> getAllStoredLearningEpisodes() {
		return episodeHistory;
	}

	
	@Override
	public List<QValue> getQs(State s) {
		List<QValue> qs = this.modelPlanner.getQs(s);
		for(QValue q : qs){
			
			//if Q for unknown action, use value initialization of curent state
			if(!this.model.transitionIsModeled(s, (GroundedAction)q.a)){
				q.q = this.modelPlanner.getValueFunctionInitialization().qValue(s, q.a);
			}
			
			//update action to real world action
			Action realWorldAction = this.domain.getAction(q.a.actionName());
			GroundedAction nga = new GroundedAction(realWorldAction, q.a.params);
			q.a = nga;
			
		}
		return qs;
	}


	@Override
	public QValue getQ(State s, AbstractGroundedAction a) {
		
		QValue q = this.modelPlanner.getQ(s, a);
		
		//if Q for unknown action, use value initialization of curent state
		if(!this.model.transitionIsModeled(s, (GroundedAction)q.a)){
			q.q = this.modelPlanner.getValueFunctionInitialization().qValue(s, q.a);
		}
		
		//update action to real world action
		Action realWorldAction = this.domain.getAction(q.a.actionName());
		GroundedAction nga = new GroundedAction(realWorldAction, q.a.params);
		q.a = nga;
		return q;
	}
	
	
	public void resetSolver(){
		this.model.resetModel();
		this.modelPlanner.resetSolver();
		this.episodeHistory.clear();
	}
	
	
	/**
	 * The value funciton planner that operates on the modeled world.
	 * @author James MacGlashan
	 *
	 */
	protected class ARTDPPlanner extends ValueFunctionPlanner{

		/**
		 * Initializes
		 * @param domain the modeled domain
		 * @param rf the modeled reward function
		 * @param tf the modeled terminal function
		 * @param gamma the discount factor
		 * @param hashingFactory the hashing factory
		 * @param vInit the constant value function initialization to use
		 */
		public ARTDPPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, double vInit){
			this(domain, rf, tf, gamma, hashingFactory, new ValueFunctionInitialization.ConstantValueFunctionInitialization(vInit));
		}
		
		
		/**
		 * Initializes
		 * @param domain the modeled domain
		 * @param rf the modeled reward function
		 * @param tf the modeled terminal function
		 * @param gamma the discount factor
		 * @param hashingFactory the hashing factory
		 * @param vInit the value function initialization to use
		 */
		public ARTDPPlanner(Domain domain, RewardFunction rf, TerminalFunction tf, double gamma, StateHashFactory hashingFactory, ValueFunctionInitialization vInit){
			VFPInit(domain, rf, tf, gamma, hashingFactory);
			
			//don't cache transition dynamics because our leanred model keeps changing!
			this.useCachedTransitions = false;
			
			this.valueInitializer = vInit;
		}
		
		@Override
		public void planFromState(State initialState) {
			throw new UnsupportedOperationException("This method should not be called for the inner ARTDP planner");
		}
		
		
		
	}



	
	

}
