from django.shortcuts import *
from settings import *
import json
from django.contrib.auth.decorators import login_required
import os
import uuid

def getData(parsefile, resultfile, uid, category):
	data = []
	data2 = []
	if (os.path.isfile(parsefile) and os.path.isfile(resultfile)):	
		
		resultFP = open(resultfile)
		result_data = json.load(resultFP)
		resultFP.close()
		
		parseFP = open(parsefile)
		parse_data = json.load(parseFP)
		tag_urls = {}
		getNodes(parse_data, 0, tag_urls)
		parseFP.close()
		tests = result_data["results"]
		for test in tests:
			if test['category'] == category and len(test['tags']) > 0:
				
				for tag in test['tags']:
						
					actual_tag = tag_urls[tag['tag']]
					attr = []
					if 'attributes' in actual_tag:
						attr = actual_tag['attributes']
					for a in attr:
						if 'Page' in a:
							tag['page'] = a['Page']
					tag['tagName'] = tag_urls[tag['tag']]['tagName']
				
			if test['category'] == category and len(test['tags']) > 0:
				test2 = {}
				test2['title'] = test['title']
				test2['tags'] = []
				ntest = 0
				npass = 0
				nfail = 0
				nins = 0
				for tag in test['tags']:
					ntest += 1
					if tag['result'] == 1:
						npass += 1
					elif tag['result'] == 2:
						nfail += 1
					else:
						nins += 1			
					if tag['result'] != 1:
						test2['tags'].append(tag)
				test['ntest'] = ntest
				test['npass'] = npass
				test['nfail'] = nfail
				test['nins'] = nins		
				if test2['tags']:		
					data2.append(test2)
				data.append(test)	
	return [data, data2, tag_urls]

def writeTag(parsefile, tagName):
	parseFP = open(parsefile)
	parse_data = json.load(parseFP)
	parseFP.close()
	
	content = parse_data["content"]
	for sect in content:
		if sect["tagName"] == tagName:
			tags = sect
	
	if len(tags["content"]) > 0:
		js = "<p><a href=\"javascript:check_all()\" style=\"padding-right: 30px;\">Expand All</a><a href=\"javascript:uncheck_all()\">Collapse All</a></p>"	
		return js + writeTree(tags, 0, 0)
	else:
		return "<p>No tags found</p>"
	
def writeTree(node, depth, count, url='node_'):
	nodetag = node["tagName"]
	url += unicode(count) + ":" + unicode(nodetag) 
	output = ""
	if depth > 0:
		output += "<div class=\"treestyle\"><ul><input type=\"checkbox\" id=\"elem-" + url + "\" checked=\"checked\"/><label for=\"elem-" + url + "\"><b><a name = \"" + url + "\" id = \"" + url + "\">" + nodetag + "</a></b></label>\n"
	url += "-"
	
	if depth > 0:
		attr = []
		for i in node["attributes"]:
			for j, k in i.iteritems():
				attr.append(unicode(j) + "=" + unicode(k))
		if attr:
			output += "("
			output += ", ".join(attr)
			output += ")"
	
	output += "\n<ul>"
	count = 0
	for i in node["content"]:	
		if not isinstance(i, basestring) and not isinstance(i, int):
			output += writeTree(i, depth + 1, count, url)
		else:
			output += "<li>" + unicode(i) + "</li>"
		count += 1	
	if depth > 0:
		output += "</ul></ul></div>"
	return output

def getTable(parsefile, resultfile):
	parseFP = open(parsefile)
	parse_data = json.load(parseFP)
	parseFP.close()
	content = parse_data["content"]
	for sect in content:
		if sect["tagName"] == "tags":
			tags = sect
	list = []
	getNodesByName(tags, "Table", list)		
	output = ""
	for i in range(len(list)):
		output += "<p>Table" + unicode(i+1) + "</p>"
		output += drawTable(list[i])
		
	resultFP = open(resultfile)
	result_data = json.load(resultFP)
	resultFP.close()	
	tests = result_data["results"]
	data = []
	for test in tests:
		if test['category'] == 5 and len(test['tags']) > 0:
			ntest = 0
			npass = 0
			nfail = 0
			nins = 0
			for tag in test['tags']:
				ntest += 1
				if tag['result'] == 1:
					npass += 1
				elif tag['result'] == 2:
					nfail += 1
				else:
					nins += 1			
			test['ntest'] = ntest
			test['npass'] = npass
			test['nfail'] = nfail
			test['nins'] = nins
			data.append(test)	
	return [data, output]
	
def getNodesByName(base, tagType, curr):	
	if base["tagName"] == tagType:
		curr.append(base)
	for node in base["content"]:
		if not isinstance(node, basestring) and not isinstance(node, int):
			getNodesByName(node, tagType, curr)

def drawTable(tag):
	output = ""
	output += "<" + unicode(tag["tagName"]) + ">"
	for node in tag['content']:
		if not isinstance(node, basestring) and not isinstance(node, int):
			output += drawTable(node)
		else:
			output += unicode(node)
	output += "</" + unicode(tag["tagName"]) + ">"
	return output
		
def parsespecific(file, tag_type):
	import json
	from pprint import pprint
	
	json_data = open (file)
	data = json.load(json_data)
	contentnode = data["content"]

	for c in contentnode:
		if (c["tagName"] == tag_type):
			return c
	return None
			
def writeNode (node, depth=0):
	print "depth =" + unicode(depth)
	nodetag = node["tagName"]
	print nodetag
	output = "<div class='node n_" + unicode(depth) + "'><b>" + nodetag + "</b><br />\n<i>\n"
	
	attr = []
	for i in node["attributes"]:
		for j, k in i.iteritems():
			attr.append(unicode(j) + "=" + unicode(k))
	output += ", ".join(attr)
	output += "</i><br />\n"
	for i in node["content"]:
		if not isinstance(i, basestring) and not isinstance(i, int):
			if i.has_key('text'):
				print ""
				output += unicode(i['text'])
			else:
				output += writeNode(i, depth + 1)
		else:
			output += unicode(i)
	output += "</div>"
	return output

def writeNode2 (node, tagName, bool=False, depth=0, count=0, url='node_'):
	#print "depth =" + unicode(depth)
	nodetag = node["tagName"]
	url += unicode(count) + ":" + unicode(nodetag) 
	#print nodetag
	if (nodetag == tagName):
		bool = True
	uid = unicode(uuid.uuid4());
	output = ""
	if bool:
		output += "<div class=\"treestyle\"><ul><input type=\"checkbox\" id=\"elem-" + uid + "\" checked=\"checked\"/><label for=\"elem-" + uid + "\"><b><a name = \"" + url + "\" id = \"" + url + "\">" + nodetag + "</a></b></label>\n"
	url += "-"
	attr = []
	for i in node["attributes"]:
		for j, k in i.iteritems():
			attr.append(unicode(j) + "=" + unicode(k))
	if bool:
		output += ", ".join(attr)
		output += "\n<ul>"
	count = 0
	for i in node["content"]:	
		if not isinstance(i, basestring) and not isinstance(i, int):
			if i.has_key('text'):
				print ""
				if bool:
					output += "<li>" + unicode(i['text']) + "</li>"
			else:
				output += writeNode2(i, tagName, bool, depth + 1, count, url)
		else:
			if bool:
				output += "<li>" + unicode(i) + "</li>"
		count += 1	
	if bool:
		output += "</ul></li></ul></div>"
	return output

	
	
def searchNode (node, tagName, depth=0, a=[]):
	nodetag = node["tagName"]
	if (nodetag == tagName):
		a.append(node)
	else:
		for i in node["content"]:
			if not isinstance(i, basestring) and not isinstance(i, int):
				searchNode(i, tagName, depth + 1, a)		

def getNodes(node, count=0, dict={}, url='#'):
	nodetag = node["tagName"]
	url += unicode(count) + ":" + unicode(nodetag)
	dict[url] = node
	url += "/"
	count = 0
	for i in node["content"]:
		if not isinstance(i, basestring) and not isinstance(i, int):
			getNodes(i, count, dict, url)
		count += 1
def countNode (node):
	total = 0
	for i in node["content"]:
		if not isinstance(i, basestring) and not isinstance(i, int):
			total += 1 + countNode(i)
	return total

def generateFormData(parsefile, resultfile):
	if os.path.isfile(parsefile):
		filePointer = open(parsefile)
		parsedata = json.load(filePointer)
		filePointer.close()
		tag_urls = {}
		getNodes(parsedata, 0, tag_urls)
	if os.path.isfile(resultfile):
		filePointer = open(resultfile)
		resultdata = json.load(filePointer)
		filePointer.close()
		url_list = []
		page_list = []
		name_list = []
		tooltip_list = []
		rule_list = []
		result_list = []
		count = 0
		for result in resultdata['results']:
			if result['category'] == 3:
				#output += unicode(result) + "<br><br>"
				rule_list.append(result['title'])
				tag_count = 0
				for tag in result['tags']:
					if count == 0:
						tag_url = unicode(tag['tag'])
						parsed_tag = tag_urls[tag_url]
						attr = parsed_tag['attributes']
						content = parsed_tag['content']
						if len(attr) > 0:
							page = attr[0]['Page']
						else:
							page = 'None'
						[name, tooltip] = getNameTooltip(content)
						url_list.append(tag_url)
						page_list.append(page)
						name_list.append(name)
						tooltip_list.append(tooltip)
					result_list.append([])	
					result_list[tag_count].append(tag)
					tag_count += 1	
				count += 1
	return [zip(url_list, page_list, name_list, tooltip_list, result_list), rule_list]			

def generateImageData(parsefile, resultfile, c):
	if os.path.isfile(parsefile):
		filePointer = open(parsefile)
		parsedata = json.load(filePointer)
		filePointer.close()
		tag_urls = {}
		getNodes(parsedata, 0, tag_urls)
	if os.path.isfile(resultfile):
		filePointer = open(resultfile)
		resultdata = json.load(filePointer)
		filePointer.close()
		url_list = []
		page_list = []
		alt_list = []
		rule_list = []
		result_list = []
		count = 0
		for result in resultdata['results']:
			if result['category'] == c:
				rule_list.append(result['title'])
				tag_count = 0
				for tag in result['tags']:
					if count == 0:
						tag_url = unicode(tag['tag'])
						parsed_tag = tag_urls[tag_url]
						attr = parsed_tag['attributes']
						alt = 'None'
						page = 'None'
						for a in attr:
							if 'Alt' in a:
								alt = a['Alt']
							if 'Page' in a:
								page = a['Page']	
						url_list.append(tag_url)
						page_list.append(page)
						alt_list.append(alt)
					result_list.append([])	
					result_list[tag_count].append(tag)
					tag_count += 1	
				count += 1
	return [zip(url_list, page_list, alt_list, result_list), rule_list]			

def getFormOutput(parsefile, resultfile, uid):	
	output = ""
	[lists, rule_list] = generateFormData(parsefile, resultfile)
	if len(lists) > 0 and len(lists[0]) > 0:
		output += startTable(["Form", "Page", "Name", "Tooltip", "Rule", "Result"])
		count = 1
		for url, page, name, tooltip, result in lists:
			
			if count % 2 == 0:
				output += "<tr class = \"even\">\n"
			else:
				output += "<tr>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += "<a href = \"/reports/" + uid + "/formtreeview/" + unicode(url) + "\">" + unicode(url.split(':')[-1]) + "</a>"
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(page)
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(name)
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(tooltip)
			output += "</td>\n"
				
			counter = 0
			for rule in result:
				if counter != 0:
					if count % 2 == 0:
						output += "<tr class = \"even\">\n"
					else:
						output += "<tr>\n"
				output += "<td>" + unicode(rule_list[counter]) + "</td>\n"
				output += "<td>" + getResultFromInt(rule['result']) + "</td>\n"
				if counter != 0:
					output += "</tr>\n"
				counter += 1
			output += "</tr>\n"
			count += 1
		output += endTable()	
	else:
		output += "No form elements found"
	return output

def getImageOutput(parsefile, resultfile, uid):
	output = ""
	[lists, rule_list] = generateImageData(parsefile, resultfile, 2)
	
	if len(lists) > 0 and len(lists[0]) > 0:
		output += startTable(["Tag", "Page", "Alt text", "Rule", "Result"])
		count = 1
		for url, page, alt, result in lists:
			
			if count % 2 == 0:
				output += "<tr class = \"even\">\n"
			else:
				output += "<tr>\n"
			
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += "<a href = \"/reports/" + uid + "/treeview/" + unicode(url) + "\">" + unicode(url.split(':')[-1]) + "</a>"
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(page)
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(alt)
			output += "</td>\n"
				
			counter = 0
			for rule in result:
				if counter != 0:
					if count % 2 == 0:
						output += "<tr class = \"even\">\n"
					else:
						output += "<tr>\n"
				output += "<td>" + unicode(rule_list[counter]) + "</td>\n"
				output += "<td>" + getResultFromInt(rule['result']) + "</td>\n"
				if counter != 0:
					output += "</tr>\n"
				counter += 1
			output += "</tr>\n"
			count += 1
		output += endTable()	
	else:
		output += "No image tags found"
	return output

def getLinkOutput(parsefile, resultfile, uid):
	output = ""
	[lists, rule_list] = generateImageData(parsefile, resultfile, 1)
	if len(lists) > 0 and len(lists[0]) > 0:
		output += startTable(["Link", "Page", "Alt text", "Rule", "Result"])
		count = 1
		for url, page, alt, result in lists:
			
			if count % 2 == 0:
				output += "<tr class = \"even\">\n"
			else:
				output += "<tr>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += "<a href = \"/reports/" + uid + "/treeview/" + unicode(url) + "\">" + unicode(url.split(':')[-1]) + "</a>"
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(page)
			output += "</td>\n"
				
			output += "<td rowspan = \"" + unicode(len(result)) + "\">\n"
			output += unicode(alt)
			output += "</td>\n"
				
			counter = 0
			for rule in result:
				if counter != 0:
					output += "<tr>\n"
				output += "<td>" + unicode(rule_list[counter]) + "</td>\n"
				output += "<td>" + getResultFromInt(rule['result']) + "</td>\n"
				if counter != 0:
					output += "</tr>\n"
				counter += 1
			output += "</tr>\n"
			count += 1
		output += endTable()	
	else:
		output += "No link tags found"
	return output


def startTable(header_list):
	string = "<table class=\"fancy\">\n<tr>"
	for header in header_list:
		string += "<th>" + header + "</th>\n"
	string += "</tr>\n"
	return string

def endTable():
	return "</table>\n"

def getResultFromInt(i):
	if i == 1:
		return "<FONT COLOR=\"#006400\"><b>pass</b></FONT>"
	elif i == 2:
		return "<FONT COLOR=\"#FF0000\"><b>fail</b></FONT>"
	elif i == 3:
		return "<b>warning</b>"
	elif i == 4:
		return "<b>manual inspection</b>"
	else:
		return ""

def getNameTooltip(content):
	name = "None"
	tooltip = "None"
	for tag in content:
		if (tag['tagName'] == 'Name'):
			name = tag['content'][0]
		if (tag['tagName'] == 'Tooltip'):
			tooltip = tag['content'][0]	
	return [name, tooltip]	
