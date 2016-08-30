package org.projectforge.business.teamcal.event;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.teamcal.ICSGenerator;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamEventServiceImpl implements TeamEventService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventServiceImpl.class);

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private TeamEventAttendeeDao teamEventAttendeeDao;

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  private SendMail sendMail;

  @Autowired
  private ICSGenerator icsGenerator;

  @Autowired
  private UserService userService;

  @Override
  public List<Integer> getAssignedAttendeeIds(TeamEventDO data)
  {
    List<Integer> assignedAttendees = new ArrayList<>();
    if (data != null && data.getAttendees() != null) {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        assignedAttendees.add(attendee.getId());
      }
    }
    return assignedAttendees;
  }

  @Override
  public List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee()
  {
    List<TeamEventAttendeeDO> resultList = new ArrayList<>();
    Set<Integer> addedUserIds = new HashSet<>();
    List<AddressDO> allAddressList = addressDao.internalLoadAllNotDeleted().stream()
        .sorted((address1, address2) -> address2.getFullName().compareTo(address1.getFullName()))
        .collect(Collectors.toList());
    for (AddressDO singleAddress : allAddressList) {
      if (StringUtils.isBlank(singleAddress.getEmail()) == false) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setAddress(singleAddress);
        List<PFUserDO> userWithSameMail = userService.findUserByMail(singleAddress.getEmail());
        if (userWithSameMail.size() > 0 && addedUserIds.contains(userWithSameMail.get(0).getId()) == false) {
          PFUserDO user = userWithSameMail.get(0);
          attendee.setUser(user);
          addedUserIds.add(user.getId());
        }
        resultList.add(attendee);
      }
    }
    return resultList;
  }

  @Override
  public TeamEventAttendeeDO getAttendee(Integer attendeeId)
  {
    return teamEventAttendeeDao.internalGetById(attendeeId);
  }

  @Override
  public void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign,
      Set<TeamEventAttendeeDO> itemsToUnassign)
  {
    for (TeamEventAttendeeDO assignAttendee : itemsToAssign) {
      if (assignAttendee.getId() < 0) {
        assignAttendee.setId(null);
        teamEventAttendeeDao.internalSave(assignAttendee);
        data.addAttendee(assignAttendee);
      }
    }

    if (data.getAttendees() != null && itemsToUnassign.size() > 0) {
      data.getAttendees().removeAll(itemsToUnassign);
      for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
        teamEventAttendeeDao.internalMarkAsDeleted(deleteAttendee);
      }
    }
    teamEventDao.update(data);
  }

  @Override
  public boolean sendTeamEventToAttendees(TeamEventDO data, boolean isNew, boolean hasChanges,
      Set<TeamEventAttendeeDO> addedAttendees)
  {
    final Mail msg = new Mail();
    PFUserDO user = ThreadLocalUserContext.getUser();
    if (user != null) {
      msg.setFrom(user.getEmail());
      msg.setFromRealname(user.getFullname());
    }
    String subject = "";
    String content = "";
    String attendeesString = "";
    for (TeamEventAttendeeDO attendee : data.getAttendees()) {
      attendeesString = attendeesString + attendee.toString() + " <br>";
    }

    if (isNew) {
      subject = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.subject.new");
      content = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content.new", data.getSubject(),
          data.getStartDate(), data.getLocation(), attendeesString, data.getNote());
    } else {
      subject = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.subject.update");
      content = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content.update", data.getSubject(),
          data.getStartDate(), data.getLocation(), attendeesString, data.getNote());
    }

    if (isNew == false && hasChanges == false && addedAttendees.size() > 0) {
      for (TeamEventAttendeeDO attendee : addedAttendees) {
        addAttendeeToMail(attendee, msg);
      }
      subject = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.subject.new");
      content = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content.new", data.getSubject(),
          data.getStartDate(), data.getLocation(), attendeesString, data.getNote());
    } else {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        addAttendeeToMail(attendee, msg);
      }
    }

    msg.setProjectForgeSubject(subject);
    msg.setContent(content);
    msg.setContentType(Mail.CONTENTTYPE_HTML);
    ByteArrayOutputStream icsFile = icsGenerator.getIcsFile(data);
    boolean result = false;
    try {
      result = sendMail.send(msg, icsFile.toString(StandardCharsets.UTF_8.name()), null);
    } catch (UnsupportedEncodingException e) {
      log.error("Something went wrong sending team event to attendee", e);
    }
    return result;
  }

  private void addAttendeeToMail(TeamEventAttendeeDO attendee, Mail msg)
  {
    if (attendee.getAddress() != null) {
      msg.addTo(attendee.getAddress().getEmail());
    }
    if (StringUtils.isNotBlank(attendee.getUrl())) {
      msg.addTo(attendee.getUrl());
    }
  }

}
