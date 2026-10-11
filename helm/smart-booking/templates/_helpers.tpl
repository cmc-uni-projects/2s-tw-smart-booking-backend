{{- define "sb.name" -}}{{ .Chart.Name }}{{- end -}}

{{- define "sb.fullname" -}}
{{- if contains .Chart.Name .Release.Name -}}{{ .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else -}}{{ printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}{{- end -}}
{{- end -}}

{{- define "sb.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
app.kubernetes.io/part-of: {{ include "sb.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/* selector labels for one component: include "sb.selector" (list . "backend") */}}
{{- define "sb.selector" -}}
app.kubernetes.io/name: {{ include "sb.name" (index . 0) }}
app.kubernetes.io/instance: {{ (index . 0).Release.Name }}
app.kubernetes.io/component: {{ index . 1 }}
{{- end -}}

{{- define "sb.secretName" -}}
{{- if .Values.existingSecret }}{{ .Values.existingSecret }}{{ else }}{{ include "sb.fullname" . }}{{ end -}}
{{- end -}}

{{- define "sb.dbHost" -}}
{{- if .Values.mysql.enabled }}{{ include "sb.fullname" . }}-mysql
{{- else }}{{ required "externalDatabase.host is required when mysql.enabled=false" .Values.externalDatabase.host }}{{ end -}}
{{- end -}}

{{- define "sb.dbPort" -}}
{{- if .Values.mysql.enabled }}3306{{ else }}{{ .Values.externalDatabase.port }}{{ end -}}
{{- end -}}

{{- define "sb.host" -}}
{{- .Values.ingress.host | default (urlParse .Values.publicUrl).host -}}
{{- end -}}

{{- define "sb.imagePullSecrets" -}}
{{- with .Values.global.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 2 }}
{{- end }}
{{- end -}}
