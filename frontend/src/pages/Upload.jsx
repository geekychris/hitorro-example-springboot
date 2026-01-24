import React, { useState } from 'react'
import { useDropzone } from 'react-dropzone'
import {
  Box,
  Typography,
  Paper,
  Button,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  IconButton,
  Alert,
} from '@mui/material'
import CloudUploadIcon from '@mui/icons-material/CloudUpload'
import DeleteIcon from '@mui/icons-material/Delete'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { dmsApi } from '../services/api'
import { toast } from 'react-toastify'

function Upload() {
  const [files, setFiles] = useState([])
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState({})

  const onDrop = (acceptedFiles) => {
    const newFiles = acceptedFiles.map((file) => ({
      file,
      id: Math.random().toString(36).substr(2, 9),
      status: 'pending',
      progress: 0,
    }))
    setFiles((prev) => [...prev, ...newFiles])
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
  })

  const removeFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id))
  }

  const uploadFiles = async () => {
    setUploading(true)

    for (const fileItem of files) {
      if (fileItem.status === 'uploaded') continue

      try {
        await dmsApi.uploadContent(fileItem.file, (progress) => {
          setUploadProgress((prev) => ({
            ...prev,
            [fileItem.id]: progress,
          }))
        })

        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id ? { ...f, status: 'uploaded' } : f
          )
        )
        toast.success(`${fileItem.file.name} uploaded successfully`)
      } catch (error) {
        console.error('Upload error:', error)
        toast.error(`Failed to upload ${fileItem.file.name}`)
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id ? { ...f, status: 'error' } : f
          )
        )
      }
    }

    setUploading(false)
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Upload Documents
      </Typography>

      <Paper
        {...getRootProps()}
        sx={{
          p: 4,
          mb: 3,
          textAlign: 'center',
          cursor: 'pointer',
          border: '2px dashed',
          borderColor: isDragActive ? 'primary.main' : 'grey.400',
          backgroundColor: isDragActive ? 'action.hover' : 'background.paper',
          transition: 'all 0.3s',
          '&:hover': {
            borderColor: 'primary.main',
            backgroundColor: 'action.hover',
          },
        }}
      >
        <input {...getInputProps()} />
        <CloudUploadIcon sx={{ fontSize: 64, color: 'primary.main', mb: 2 }} />
        <Typography variant="h6" gutterBottom>
          {isDragActive
            ? 'Drop files here'
            : 'Drag & drop files here, or click to select'}
        </Typography>
        <Typography variant="body2" color="textSecondary">
          Supports: PDF, Word, Excel, PowerPoint, Images, and more
        </Typography>
      </Paper>

      {files.length > 0 && (
        <Paper sx={{ p: 2, mb: 2 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Files to Upload ({files.length})
            </Typography>
            <Button
              variant="contained"
              startIcon={<CloudUploadIcon />}
              onClick={uploadFiles}
              disabled={uploading || files.every((f) => f.status === 'uploaded')}
            >
              {uploading ? 'Uploading...' : 'Upload All'}
            </Button>
          </Box>

          <List>
            {files.map((fileItem) => (
              <ListItem
                key={fileItem.id}
                secondaryAction={
                  fileItem.status === 'uploaded' ? (
                    <CheckCircleIcon color="success" />
                  ) : (
                    <IconButton
                      edge="end"
                      onClick={() => removeFile(fileItem.id)}
                      disabled={uploading}
                    >
                      <DeleteIcon />
                    </IconButton>
                  )
                }
              >
                <ListItemText
                  primary={fileItem.file.name}
                  secondary={`${(fileItem.file.size / 1024).toFixed(2)} KB - ${
                    fileItem.status
                  }`}
                />
                {uploading && fileItem.status === 'pending' && (
                  <Box sx={{ width: '100%', ml: 2 }}>
                    <LinearProgress
                      variant="determinate"
                      value={uploadProgress[fileItem.id] || 0}
                    />
                  </Box>
                )}
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Alert severity="info">
        <Typography variant="body2">
          <strong>Tip:</strong> You can upload multiple files at once. Supported
          formats include documents, images, and spreadsheets.
        </Typography>
      </Alert>
    </Box>
  )
}

export default Upload
