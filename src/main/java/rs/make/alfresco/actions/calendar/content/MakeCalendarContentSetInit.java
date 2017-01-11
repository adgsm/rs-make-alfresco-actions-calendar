package rs.make.alfresco.actions.calendar.content;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.IsSubTypeEvaluator;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.apache.log4j.Logger;

public class MakeCalendarContentSetInit extends BaseScopableProcessorExtension {

	private NodeService nodeService;
	public NodeService getNodeService() {
		return nodeService;
	}
	public void setNodeService( NodeService nodeService ) {
		this.nodeService = nodeService;
	}

	private ActionService actionService;
	public ActionService getActionService() {
		return actionService;
	}
	public void setActionService( ActionService actionService ) {
		this.actionService = actionService;
	}

	private RuleService ruleService;
	public RuleService getRuleService() {
		return ruleService;
	}
	public void setRuleService( RuleService ruleService ) {
		this.ruleService = ruleService;
	}

	private static final String RULE_NAME_INBOUND = "calendar-content-set-inbound";
	private static final String RULE_NAME_UPDATE = "calendar-content-set-update";
	private static final String ACTION_TITLE = "MakeCalendarContentSet";

	private static Logger logger = Logger.getLogger( MakeCalendarContentSetInit.class );

	public void calendarContentSetActions( NodeRef target ){
		List<Rule> existingRules = this.ruleService.getRules( target );
		for( Rule rule : existingRules ){
			if( rule.getTitle().equals( RULE_NAME_INBOUND ) ){
				ruleService.removeRule( target , rule );
			}
			else if( rule.getTitle().equals( RULE_NAME_UPDATE ) ){
				ruleService.removeRule( target , rule );
			}
		}
		calendarContentSetRule( target , RuleType.INBOUND , RULE_NAME_INBOUND , true , false , ACTION_TITLE , true );
		calendarContentSetRule( target , RuleType.UPDATE , RULE_NAME_UPDATE , true , false , ACTION_TITLE , true );
	}

	private void calendarContentSetRule( NodeRef nodeRef , String ruleType , String ruleTitle , boolean applyToChildren , boolean ruleDisabled , String actionTitle , boolean executeAsynchronously ){
		Rule rule = new Rule();
		rule.setRuleType( ruleType );
		rule.setTitle( ruleTitle );
		rule.applyToChildren( applyToChildren );
		rule.setRuleDisabled( ruleDisabled );
		rule.setExecuteAsynchronously( executeAsynchronously );

		CompositeAction compositeAction = actionService.createCompositeAction();
		rule.setAction( compositeAction );

		ActionCondition actionCondition = actionService.createActionCondition( IsSubTypeEvaluator.NAME );

		Map<String, Serializable> conditionParameters = new HashMap<String, Serializable>(1);
		conditionParameters.put( IsSubTypeEvaluator.PARAM_TYPE , MakeCalendarContentSet.PARAM_TYPE );
		actionCondition.setParameterValues( conditionParameters );

		compositeAction.addActionCondition( actionCondition );

		Action action = actionService.createAction( MakeCalendarContentSet.NAME );
		action.setTitle( actionTitle );

		action.setExecuteAsynchronously( executeAsynchronously );

		compositeAction.addAction( action );

		if( nodeRef != null ) {
			ruleService.saveRule( nodeRef , rule );
			logger.info( "Rule \"" + ruleTitle + "\" successfully saved aginst node \"" + nodeService.getProperty( nodeRef , ContentModel.PROP_NAME ) + "\"" );
		}
	}
}
