/*
 * SAE - Sistema de Agenda Electronica
 * Copyright (C) 2009  IMM - Intendencia Municipal de Montevideo
 *
 * This file is part of SAE.

 * SAE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uy.gub.imm.sae.web.mbean.administracion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.richfaces.component.UIDataTable;

import uy.gub.imm.sae.business.ejb.facade.Disponibilidades;
import uy.gub.imm.sae.common.DisponibilidadReserva;
import uy.gub.imm.sae.common.Utiles;
import uy.gub.imm.sae.common.VentanaDeTiempo;
import uy.gub.imm.sae.web.common.BaseMBean;
import uy.gub.imm.sae.web.common.RowList;


public class CrearDisponibilidadMBean extends BaseMBean {
	public static final String MSG_ID = "pantalla";
	
	
	@EJB(name="ejb/DisponibilidadesBean")
	private Disponibilidades disponibilidadesEJB;

	private SessionMBean sessionMBean;
	private CrearDispSessionMBean crearDispSessionMBean;
	
	private UIDataTable tablaDispMatutina;
	private UIDataTable tablaDispVespertina;

	private List<SelectItem> horas =  new ArrayList<SelectItem>();
	private List<SelectItem> minutos =  new ArrayList<SelectItem>();

		
	@PostConstruct
	public void initGenDisponibilidad(){
		//Se controla que se haya Marcado una agenda para trabajar con los recursos
		if (sessionMBean.getAgendaMarcada() == null){
			addErrorMessage("Debe tener una agenda seleccionada", MSG_ID);
		}
		if (sessionMBean.getRecursoMarcado() == null){
			addErrorMessage("Debe tener un recurso seleccionado", MSG_ID);
		}

		this.cargarListaHoras();
		this.cargarListaMinutos();
		//this.configurarDisponibilidadesDelDia();
	}

	public SessionMBean getSessionMBean() {
		return sessionMBean;
	}
	
	public void setSessionMBean(SessionMBean sessionMBean) {
		this.sessionMBean = sessionMBean;
	}


	
	public UIDataTable getTablaDispMatutina() {
		return tablaDispMatutina;
	}

	public void setTablaDispMatutina(UIDataTable tablaDispMatutina) {
		this.tablaDispMatutina = tablaDispMatutina;
	}

	public UIDataTable getTablaDispVespertina() {
		return tablaDispVespertina;
	}

	public void setTablaDispVespertina(UIDataTable tablaDispVespertina) {
		this.tablaDispVespertina = tablaDispVespertina;
	}


	
	public void beforePhaseCrearDisponibilidades (PhaseEvent event) {

		if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			sessionMBean.setPantallaTitulo("Generar Disponibilidades para un Día");
		}
	}

	
	public void crearDisponibilidades(ActionEvent event){
		Boolean huboError = false;
		
		if (crearDispSessionMBean.getFechaCrear() == null ) {
			addErrorMessage("La fecha en la cual se van a crear disponibilidades no puede ser nula", MSG_ID);
			huboError = true;
		}
		else {
		
			Calendar c1 = new GregorianCalendar();
			c1.setTime(crearDispSessionMBean.getFechaCrear());
			c1.set(Calendar.HOUR_OF_DAY, crearDispSessionMBean.getHoraD());
			c1.set(Calendar.MINUTE, crearDispSessionMBean.getMinD());
		
			crearDispSessionMBean.setHoraDesde(c1.getTime());
			
			if (crearDispSessionMBean.getHoraDesde() == null ) {
				addErrorMessage("La hora Desde no puede ser nula", MSG_ID);
				huboError = true;
			}
			
			Calendar c2 = new GregorianCalendar();
			c2.setTime(crearDispSessionMBean.getFechaCrear());
			c2.set(Calendar.HOUR_OF_DAY, crearDispSessionMBean.getHoraH());
			c2.set(Calendar.MINUTE, crearDispSessionMBean.getMinH());
			
			crearDispSessionMBean.setHoraHasta(c2.getTime());
			
			if (crearDispSessionMBean.getHoraHasta() == null ) {
				addErrorMessage("La hora Hasta no puede ser nula", MSG_ID);
				huboError = true;
			}
		}

		if (!huboError) {
			try{
			disponibilidadesEJB.generarDisponibilidadesNuevas(sessionMBean.getRecursoMarcado(),crearDispSessionMBean.getFechaCrear(), 
					crearDispSessionMBean.getHoraDesde(), crearDispSessionMBean.getHoraHasta(), 
					crearDispSessionMBean.getFrecuencia(), crearDispSessionMBean.getCupo());
			addInfoMessage("Disponibilidades creadas correctamente.", MSG_ID);
			
			this.configurarDisponibilidadesDelDia();
			
			}
			
			catch (OptimisticLockException lockE){
				addErrorMessage(lockE, "Al generar disponibilidades, el recurso no se encontraba disponible. Intente más tarde.");
			}
			catch (PersistenceException persE){
				addErrorMessage(persE, "Al generar disponibilidades, el recurso no se encontraba disponible. Intente más tarde.");				
			}
			catch (EJBException eE){
				if (eE.getCause() instanceof OptimisticLockException){
					addErrorMessage("Al generar disponibilidades, el recurso no se encontraba disponible. Intente más tarde.", MSG_ID);					
				}
				else{
					addErrorMessage(eE, MSG_ID);
					}
			}
			catch (Exception e) {
					addErrorMessage(e, MSG_ID);
			}

		}
		
	}

	public CrearDispSessionMBean getCrearDispSessionMBean() {
		return crearDispSessionMBean;
	}

	public void setCrearDispSessionMBean(CrearDispSessionMBean crearDispSessionMBean) {
		this.crearDispSessionMBean = crearDispSessionMBean;
	}

	
	private void cargarListaHoras(){

		horas =  new ArrayList<SelectItem>();
	    Integer h = 0;
	    String labelH;
	    
	    while (h < 24){
			SelectItem s = new SelectItem();
			s.setValue(h);
			labelH = Integer.toString(h);
			if (labelH.length()<2){
				labelH = "0"+labelH;
			}
			s.setLabel(labelH);
			horas.add(s);
			h = h + 1;
		}
	}

	private void cargarListaMinutos(){

		minutos =  new ArrayList<SelectItem>();
	    Integer h = 0;
	    String labelH;
	    
	    while (h < 60){
			SelectItem s = new SelectItem();
			s.setValue(h);
			labelH = Integer.toString(h);
			if (labelH.length()<2){
				labelH = "0"+labelH;
			}
			s.setLabel(labelH);
			minutos.add(s);
			h = h + 1;
		}
	}

	public List<SelectItem> getHoras() {
		return horas;
	}

	public void setHoras(List<SelectItem> horas) {
		this.horas = horas;
	}

	public List<SelectItem> getMinutos() {
		return minutos;
	}

	public void setMinutos(List<SelectItem> minutos) {
		this.minutos = minutos;
	}

	/*
	 * -----------------------------------------------------------------------------
	 */
	public void consultarDisponibilidadesDelDia(ActionEvent event) {
		
		if (crearDispSessionMBean.getFechaCrear() == null ) {
			addErrorMessage("La fecha en la cual se van a crear disponibilidades no puede ser nula", MSG_ID);
		}
		else{
			this.configurarDisponibilidadesDelDia();
		}
	}
	
	
	public void configurarDisponibilidadesDelDia() {

		List<DisponibilidadReserva> dispMatutinas   = new ArrayList<DisponibilidadReserva>();
		List<DisponibilidadReserva> dispVespertinas = new ArrayList<DisponibilidadReserva>();

		VentanaDeTiempo ventana = new VentanaDeTiempo();
		ventana.setFechaInicial(Utiles.time2InicioDelDia(crearDispSessionMBean.getFechaCrear()));
		ventana.setFechaFinal(Utiles.time2FinDelDia(crearDispSessionMBean.getFechaCrear()));
			
		try {
			List<DisponibilidadReserva> lista = disponibilidadesEJB.obtenerDisponibilidadesReservas(sessionMBean.getRecursoMarcado(), ventana);
				
			for (DisponibilidadReserva d : lista) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(d.getHoraInicio());
					
					if (cal.get(Calendar.AM_PM) == Calendar.AM) {
						//Matutino
						dispMatutinas.add(d);
					}
					else {
						//Vespertino
						dispVespertinas.add(d);
					}
			}
				
				
		} catch (Exception e) { 
			addErrorMessage(e);
		}

		crearDispSessionMBean.setDisponibilidadesDelDiaMatutina(new RowList<DisponibilidadReserva>(dispMatutinas));
		crearDispSessionMBean.setDisponibilidadesDelDiaVespertina(new RowList<DisponibilidadReserva>(dispVespertinas));
	}

}
