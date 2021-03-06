package org.projectforge.web.vacation;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.components.DatePanel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.vaynberg.wicket.select2.Select2Choice;

@PrepareForTest({ DatePanel.class, Form.class, ThreadLocalUserContext.class, ConfigXml.class })
public class VacationFormValidatorTest extends PowerMockTestCase
{
  private EmployeeDO employee;

  private VacationService vacationService;

  private Calendar startDate;

  private Calendar endDate;

  private DatePanel startDatePanel;

  private DatePanel endDatePanel;

  private DropDownChoice<VacationStatus> statusChoice;

  private Select2Choice<EmployeeDO> employeeSelect;

  @BeforeMethod
  public void setUp()
  {
    this.employee = mock(EmployeeDO.class);
    when(this.employee.getUrlaubstage()).thenReturn(30);
    this.vacationService = mock(VacationService.class);
    this.startDate = new GregorianCalendar();
    this.endDate = new GregorianCalendar();
    this.startDatePanel = mock(DatePanel.class);
    this.endDatePanel = mock(DatePanel.class);
    this.statusChoice = mock(DropDownChoice.class);
    this.employeeSelect = mock(Select2Choice.class);
    mockStatic(ThreadLocalUserContext.class);
    mockStatic(ConfigXml.class);
    Locale locale = Locale.getDefault();
    TimeZone timeZone = TimeZone.getDefault();
    ConfigXml configXml = new ConfigXml("./target/Projectforge");
    when(ThreadLocalUserContext.getLocale()).thenReturn(locale);
    when(ThreadLocalUserContext.getTimeZone()).thenReturn(timeZone);
    when(ConfigXml.getInstance()).thenReturn(configXml);
    Calendar vacationEndDate = new GregorianCalendar();
    vacationEndDate.set(Calendar.MONTH, Calendar.MARCH);
    vacationEndDate.set(Calendar.DAY_OF_MONTH, 31);
    when(this.vacationService.getEndDateVacationFromLastYear()).thenReturn(vacationEndDate);
  }

  @Test
  public void afterMarchTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.APRIL);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.APRIL);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void afterMarchNegativeTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.APRIL);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.APRIL);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(30));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(1)).error(any());
  }

  @Test
  public void beforeMarchTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.MARCH);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.MARCH);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchNoPreviousYearVacationTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.MARCH);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.MARCH);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchWithPreviousYearVacationTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.MARCH);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.MARCH);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchNoPreviousYearNegativVacationTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.MARCH);
    this.startDate.set(Calendar.DAY_OF_MONTH, 3);
    this.endDate.set(Calendar.MONTH, Calendar.MARCH);
    this.endDate.set(Calendar.DAY_OF_MONTH, 13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(30));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(1)).error(any());
  }

  @Test
  public void overMarchWithPreviousYearVacationTest()
  {
    this.startDate.set(Calendar.MONTH, Calendar.MARCH);
    this.startDate.set(Calendar.DAY_OF_MONTH, 30);
    this.endDate.set(Calendar.MONTH, Calendar.APRIL);
    this.endDate.set(Calendar.DAY_OF_MONTH, 10);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.get(Calendar.YEAR))).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
        .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  private VacationFormValidator createValidator()
  {
    VacationDO newVacationData = new VacationDO();
    newVacationData.setEmployee(this.employee);
    newVacationData.setStartDate(startDate.getTime());
    newVacationData.setEndDate(endDate.getTime());

    VacationFormValidator validator = new VacationFormValidator(vacationService, newVacationData);
    when(startDatePanel.getConvertedInput()).thenReturn(startDate.getTime());
    when(endDatePanel.getConvertedInput()).thenReturn(endDate.getTime());
    when(statusChoice.getConvertedInput()).thenReturn(VacationStatus.IN_PROGRESS);
    when(employeeSelect.getConvertedInput()).thenReturn(this.employee);
    validator.getDependentFormComponents()[0] = startDatePanel;
    validator.getDependentFormComponents()[1] = endDatePanel;
    validator.getDependentFormComponents()[2] = statusChoice;
    validator.getDependentFormComponents()[3] = employeeSelect;
    return validator;
  }

}
