package com.tedza

import grails.converters.JSON

import org.apache.commons.lang.StringUtils
import org.springframework.dao.DataIntegrityViolationException

class VideoController {
	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
	def videoService
	
	enum Type {
		TECHNOLOGY("Hightech"), DESIGN("Design"), ENGAGED("Engaged"), SCIENCE("Science"), STARTUP("Startup"), TRAVEL("Travel")
		final String id
		private Type(String id) { this.id = id }
		static Type byId(String id) {
			values().find { it.id == id }
		}
		String toString() {
			return StringUtils.capitalize(name().toLowerCase())
		}
	}

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [videoInstanceList: Video.list(params), videoInstanceTotal: Video.count()]
    }

    def create() {
        [videoInstance: new Video(params)]
    }

    def save() {
        def videoInstance = new Video(params)
        if (!videoInstance.save(flush: true)) {
            render(view: "create", model: [videoInstance: videoInstance])
            return
        }

		flash.message = message(code: 'default.created.message', args: [message(code: 'video.label', default: 'Video'), videoInstance.id])
        redirect(action: "show", id: videoInstance.id)
    }

    def show() {
        def videoInstance = Video.get(params.id)
        if (!videoInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "list")
            return
        }

        [videoInstance: videoInstance]
    }

    def edit() {
        def videoInstance = Video.get(params.id)
        if (!videoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "list")
            return
        }

        [videoInstance: videoInstance]
    }

    def update() {
        def videoInstance = Video.get(params.id)
        if (!videoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (videoInstance.version > version) {
                videoInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'video.label', default: 'Video')] as Object[],
                          "Another user has updated this Video while you were editing")
                render(view: "edit", model: [videoInstance: videoInstance])
                return
            }
        }

        videoInstance.properties = params

        if (!videoInstance.save(flush: true)) {
            render(view: "edit", model: [videoInstance: videoInstance])
            return
        }

		flash.message = message(code: 'default.updated.message', args: [message(code: 'video.label', default: 'Video'), videoInstance.id])
        redirect(action: "show", id: videoInstance.id)
    }

    def delete() {
        def videoInstance = Video.get(params.id)
        if (!videoInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "list")
            return
        }

        try {
            videoInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'video.label', default: 'Video'), params.id])
            redirect(action: "show", id: params.id)
        }
    }

	def grabVideos() {
		def finished = false
		while(!finished) {
		    try {
		        finished = videoService.crawl()
		    }
		    catch(Exception e) {
		        println e.getMessage()
		        println "DEU ERRO NESSA PORRA!"
		        finished = false
		    }
		}
	}

    def grabThemes() {
        try {
            println "Vai pegar themes"
            videoService.crawlThemes()
            println "Terminou a pesquisa de themes"
        } catch(Exception e){
            println "Terminou até terminou... MAS DEU PAU!!!!! >:) "
            println e
        }
    }
	
	def getResults() {
		

        println "\n\n\n" + params.dump()
		
		Type type = Type.byId(q1)
        def totalDuration = Integer.parseInt(params.q2.split(" ")[0]) * 60
        println "q1: " + type + "   duration: " + totalDuration + "    q3: " + q3
		
		def videos = Video.executeQuery("SELECT new Map(v.id as id, v.title as title, v.duration as duration, v.low as low, v.medium as medium, v.high as high, v.date as date, v.event as event) FROM Video AS v, IN (v.tags) AS t, IN (v.themes) AS th " +
										"WHERE t.name = '${type}' AND th.name = '${q3}' AND v.duration <= ${totalDuration} " +
										"order by random()")
		println videos.size()

        int duration = 0
		List<Video> videoPlaylist = new ArrayList<Video>()
		for (video in videos) {
			// if the video duration plus the sum of playlist exceeds the total, 
			// skip to the next
			if ((video.duration + duration) > totalDuration) continue
			
			duration += video.duration
			videoPlaylist.add(video)
			
			// if the difference between the total minutes and playlist
			// is lower than 5 minutes, the playlist is done
			if ((totalDuration - duration) < 180) break
		}

		render videoPlaylist as JSON
	}
}
