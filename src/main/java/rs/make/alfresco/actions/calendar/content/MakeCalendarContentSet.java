package rs.make.alfresco.actions.calendar.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.calendar.CalendarModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.calendar.CalendarEntry;
import org.alfresco.service.cmr.calendar.CalendarService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;

public class MakeCalendarContentSet extends ActionExecuterAbstractBase  {

	private NodeService nodeService;
	public NodeService getNodeService() {
		return nodeService;
	}
	public void setNodeService( NodeService nodeService ) {
		this.nodeService = nodeService;
	}

	private Repository repositoryHelper;
	public Repository getRepositoryHelper() {
		return repositoryHelper;
	}
	public void setRepositoryHelper( Repository repositoryHelper ) {
		this.repositoryHelper = repositoryHelper;
	}

	private SiteService siteService;
	public SiteService getSiteService() {
		return siteService;
	}
	public void setSiteService( SiteService siteService ) {
		this.siteService = siteService;
	}

	protected CalendarService calendarService;
	public CalendarService getCalendarService() {
		return calendarService;
	}
	public void setCalendarService( CalendarService calendarService ) {
		this.calendarService = calendarService;
	}

	protected ContentService contentService;
	public ContentService getContentService() {
		return contentService;
	}
	public void setContentService( ContentService contentService ) {
		this.contentService = contentService;
	}

	protected DictionaryService dictionaryService;
	public DictionaryService getDictionaryService() {
		return dictionaryService;
	}
	public void setDictionaryService( DictionaryService dictionaryService ) {
		this.dictionaryService = dictionaryService;
	}

	private static Logger logger = Logger.getLogger( MakeCalendarContentSet.class );

	private static final String DEFAULT_TIME_ZONE = "Etc/GMT+0";
	private static final String CALENDAR_MIMETYPE = "text/calendar";

	public static final String NAME = "MakeCalendarContentSet";
	public static final QName PARAM_TYPE = CalendarModel.TYPE_EVENT;

	@Override
	protected void executeImpl( Action action , NodeRef actionedUponNodeRef ) {
		if ( nodeService.exists( actionedUponNodeRef ) == true ) {
			try{
				String name = nodeService.getProperty( actionedUponNodeRef , ContentModel.PROP_NAME ).toString();
				logger.debug( "[" + MakeCalendarContentSet.class.getName() + "] Processing \"" + name + "\"." );

				String siteShortName = null;

				NodeRef companyHomeRef = repositoryHelper.getCompanyHome();
				NodeRef parent = nodeService.getPrimaryParent( actionedUponNodeRef ).getParentRef();
				while( !dictionaryService.isSubClass( nodeService.getType( parent ) , SiteModel.TYPE_SITE ) && !parent.equals( companyHomeRef ) ){
					parent = nodeService.getPrimaryParent( parent ).getParentRef();
				}
				if( parent.equals( companyHomeRef ) == false ){
					siteShortName = siteService.getSite( parent ).getShortName();
				}
				else{
					String errorMessage = "Node \"" + name + "\" does not belong to any site and so doesn't have calendar.";
					throw new Exception( errorMessage );
				}

				logger.debug( "[" + MakeCalendarContentSet.class.getName() + "] Looking for \"" + name + "\" calendar entry in \"" + siteShortName + "\" website." );

				CalendarEntry calendarEntry = calendarService.getCalendarEntry( siteShortName , name );

				byte[] calendarExportContent = makeCalendarExport( calendarEntry );

				ContentWriter writer = contentService.getWriter( actionedUponNodeRef , ContentModel.PROP_CONTENT, true );
				writer.setMimetype( CALENDAR_MIMETYPE );
				writer.putContent( new ByteArrayInputStream( calendarExportContent ) );
			}
			catch( Exception e ){
				logger.error( "[" + MakeCalendarContentSet.class.getName() + "] Process of executing " + NAME + " rule failed." );
				logger.error( "[" + MakeCalendarContentSet.class.getName() + "] Error message: " + e.getMessage() );
			}
			logger.debug( "[" + MakeCalendarContentSet.class.getName() + "] Rule \"" + NAME + "\" applied against node \"" + nodeService.getProperty( actionedUponNodeRef , ContentModel.PROP_NAME ) + "\"." );
		}
	}

	@Override
	protected void addParameterDefinitions( List<ParameterDefinition> paramList ) {
	}

	private byte[] makeCalendarExport( CalendarEntry calendarEntry ) throws IOException, ValidationException, URISyntaxException, ParseException{
		Calendar calendar = new Calendar();
		calendar.getProperties().add( new ProdId( "-//WBIF//iCal4j 1.0//EN" ) );
		calendar.getProperties().add( Version.VERSION_2_0 );
		calendar.getProperties().add( CalScale.GREGORIAN );

		TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
		TimeZone timeZone = timeZoneRegistry.getTimeZone( DEFAULT_TIME_ZONE );
		VTimeZone vTimeZone = timeZone.getVTimeZone();

		String title = calendarEntry.getTitle();

		java.util.Calendar startDate = new GregorianCalendar();
		startDate.setTimeZone( timeZone );
		startDate.setTime( calendarEntry.getStart() );
		java.util.Calendar endDate = new GregorianCalendar();
		endDate.setTimeZone( timeZone );
		endDate.setTime( calendarEntry.getEnd() );

		DateTime eventStart = new DateTime( startDate.getTime() );
		DateTime eventEnd = new DateTime( endDate.getTime() );
		VEvent event = new VEvent( eventStart , eventEnd , title );
		event.getProperties().add( vTimeZone.getTimeZoneId() );

		event.getProperties().add( new Location( calendarEntry.getLocation() ) );
		event.getProperties().add( new Description() );
		event.getProperties().getProperty( Property.DESCRIPTION ).setValue( calendarEntry.getDescription() );

		UidGenerator ug = new UidGenerator( "uidGen" );
		Uid uid = ug.generateUid();
		event.getProperties().add( uid );

		calendar.getComponents().add( event );

		CalendarOutputter outputter = new CalendarOutputter();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		outputter.output( calendar, baos );
		return baos.toByteArray();
	}
}
