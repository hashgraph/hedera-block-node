{{/*
Expand the name of the chart.
*/}}
{{- define "blockstream-simulator-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "blockstream-simulator-chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "blockstream-simulator-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "blockstream-simulator-chart.labels" -}}
helm.sh/chart: {{ include "blockstream-simulator-chart.chart" . }}
{{ include "blockstream-simulator-chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "blockstream-simulator-chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "blockstream-simulator-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
This function returns the image tag from the values.yaml file if provided.
If the tag is not provided, it defaults to the AppVersion specified in the Chart.yaml file.
Usage: {{ include "image.AppVersion" . }}
*/}}
{{- define "blockstream-simulator-chart.image.tag" -}}
{{- default .Chart.AppVersion .Values.image.tag -}}
{{- end -}}

